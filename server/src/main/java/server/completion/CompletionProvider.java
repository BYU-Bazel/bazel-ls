package server.completion;

import com.google.common.collect.ImmutableList;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPI;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPIException;
import server.bazel.tree.BuildTarget;
import server.bazel.tree.SourceFile;
import server.bazel.tree.WorkspaceTree;
import server.utils.*;
import server.workspace.Workspace;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CompletionProvider {
    private static final Logger logger = LogManager.getLogger(CompletionProvider.class);

    private StarlarkWizard wizard = null;
    private DocumentTracker tracker = null;
    private FileRepository fileRepo = null;

    public CompletionProvider() {
        super();
    }

    public StarlarkWizard getWizard() {
        return wizard;
    }

    public void setWizard(StarlarkWizard wizard) {
        this.wizard = wizard;
    }

    public DocumentTracker getTracker() {
        return tracker;
    }

    public void setTracker(DocumentTracker tracker) {
        this.tracker = tracker;
    }

    public FileRepository getFileRepo() {
        return fileRepo;
    }

    public void setFileRepo(FileRepository fileRepo) {
        this.fileRepo = fileRepo;
    }

    private FileRepository fileRepo() {
        return fileRepo != null ? fileRepo : FileRepository.getDefault();
    }

    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> provide(CompletionParams params) {
        final WorkspaceAPI api;
        {
            final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
            api = new WorkspaceAPI(tree);
        }

        final Path absWorkspacePath;
        {
            absWorkspacePath = Workspace.getInstance().getRootFolder().getPath().toAbsolutePath();
        }

        final Path absDocumentPath;
        {
            final URI fileURI = URI.create(params.getTextDocument().getUri());
            absDocumentPath = Paths.get(fileURI).toAbsolutePath();
        }

        final Path absPackagePath;
        {
            absPackagePath = absDocumentPath.getParent();
        }

        final List<CompletionItem> completions = new ArrayList<>();

        if (!absDocumentPath.toString().endsWith("BUILD") && !absDocumentPath.toString().endsWith("BUILD.bazel")) {
            return completed(completions);
        }

        final String currentLine;
        {
            final URI uri = URI.create(params.getTextDocument().getUri());
            final String content = getTracker().getContents(uri);
            final List<String> lines = Arrays.asList(content.split("\n"));
            currentLine = lines.get(params.getPosition().getLine());
            logger.info("Current line: " + currentLine);
        }

        boolean shouldAutocomplete = false;
        String toAutocomplete = null;
        {
            final Pattern pattern = Pattern.compile(TriggerCharacters.QUOTE_REGEX);
            final Matcher matcher = pattern.matcher(currentLine);
            final int triggerIdx = params.getPosition().getCharacter() - 1;

            while (matcher.find()) {
                final int matchStart = matcher.start();
                final int matchEnd = matcher.end() - 1;

                final boolean frontMatchesEnd = currentLine.charAt(matchStart) == currentLine.charAt(matchEnd);
                if (frontMatchesEnd && triggerIdx == matchEnd && matchEnd != matchStart) {
                    break;
                }

                if (triggerIdx >= matchStart && triggerIdx <= matchEnd) {
                    shouldAutocomplete = true;
                    toAutocomplete = currentLine.substring(matchStart, matchEnd + 1).substring(1);
                    break;
                }
            }
        }

        {
            if (!shouldAutocomplete) {
                return completed(completions);
            }

            if (toAutocomplete.startsWith("@")) {
                return completed(completions);
            }
        }

        final Path rollingPath;
        boolean completeBuildTargets = false;
        if (toAutocomplete.startsWith(TriggerCharacters.DOUBLE_SLASH)) {
            final int pkgStartIdx = TriggerCharacters.DOUBLE_SLASH.length();

            if (toAutocomplete.contains(TriggerCharacters.COLON)) {
                final int pkgEndIdx = toAutocomplete.indexOf(TriggerCharacters.COLON);
                final String pkgPath = toAutocomplete.substring(pkgStartIdx, pkgEndIdx);

                final int localStartIdx = pkgEndIdx + 1;
                final String localPath = toAutocomplete.substring(localStartIdx);

                completeBuildTargets = !localPath.contains(TriggerCharacters.SINGLE_SLASH);
                rollingPath = joinPaths(absWorkspacePath.toString(), pkgPath, localPath);
            } else {
                final String pkgPath = toAutocomplete.substring(pkgStartIdx);
                rollingPath = joinPaths(absWorkspacePath.toString(), pkgPath);
            }
        } else if (toAutocomplete.startsWith(TriggerCharacters.COLON)) {
            final int localStartIdx = toAutocomplete.indexOf(TriggerCharacters.COLON) + 1;
            final String localPath = toAutocomplete.substring(localStartIdx);

            completeBuildTargets = !localPath.contains(TriggerCharacters.SINGLE_SLASH);
            rollingPath = joinPaths(absWorkspacePath.toString(), localPath);
        } else {
            rollingPath = joinPaths(absWorkspacePath.toString(), toAutocomplete);
        }

        {
            if (!fileRepo().exists(rollingPath)) {
                logger.info(rollingPath + " doesnt exist");
                return completed(completions);
            }

            if (fileRepo().isFile(rollingPath)) {
                logger.info(rollingPath + " is a file");
                return completed(completions);
            }
        }

        {
            File directory = new File(rollingPath.toString());
            String[] children = Nullability.nullableOr(new String[]{}, directory::list);

            for (String child : children) {
                final CompletionItem item = new CompletionItem();
                item.setLabel(child);

                Path childPath = joinPaths(rollingPath.toString(), child);
                if (fileRepo().isDir(childPath)) {
                    item.setKind(CompletionItemKind.Folder);
                } else {
                    item.setKind(CompletionItemKind.File);
                }

                completions.add(item);
            }
        }

        final Path buildFile = getBuildFile(rollingPath);
        if (completeBuildTargets && buildFile != null) {
            StarlarkFile file = null;
            try {
                final String content = tracker.getContents(buildFile.toUri());
                final ParserInput input = ParserInput.fromString(content, buildFile.toUri().toString());
                file = StarlarkFile.parse(input);
            } catch (Error | RuntimeException e) {
                logger.error("Parsing failed for an unknown reason!");
                logger.error(Logging.stackTraceToString(e));
            }

            if (file != null) {
                for (StarlarkWizard.TargetMeta meta : getWizard().locateTargets(file)) {
                    final CompletionItem item = new CompletionItem();
                    item.setLabel(meta.name().getValue());
                    item.setKind(CompletionItemKind.Value);
                    completions.add(item);
                }
            }
        }

        return completed(completions);
    }

    private Path getBuildFile(Path pkgPath) {
        final Path buildBazelFile = joinPaths(pkgPath.toString(), "BUILD.bazel");
        final Path buildFile = joinPaths(pkgPath.toString(), "BUILD");

        if (fileRepo().exists(buildBazelFile)) {
            return buildBazelFile;
        }

        if (fileRepo().exists(buildFile)) {
            return buildFile;
        }

        return null;
    }

    private Path joinPaths(String... paths) {
        StringBuilder builder = new StringBuilder();

        for (final String p : paths) {
            builder.append(p);
            builder.append('/');
        }

        return Paths.get(builder.toString()).normalize();
    }

    private CompletableFuture<Either<List<CompletionItem>, CompletionList>> completed(List<CompletionItem> items) {
        final Either<List<CompletionItem>, CompletionList> either = Either.forLeft(items);
        return CompletableFuture.completedFuture(either);
    }
}
