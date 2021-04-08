package server.completion;

import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
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

    private FileRepository effectiveFileRepo() {
        return getFileRepo() != null ? getFileRepo() : FileRepository.getDefault();
    }

    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> provide(CompletionParams params) {
        final List<CompletionItem> completions = new ArrayList<>();

        // The absolute path to this WORKSPACE.
        final Path absWorkspacePath;
        {
            absWorkspacePath = Workspace.getInstance().getRootFolder().getPath().toAbsolutePath();
        }

        // The absolute path to the currently opened document.
        final Path absDocumentPath;
        {
            final URI fileURI = URI.create(params.getTextDocument().getUri());
            absDocumentPath = Paths.get(fileURI).toAbsolutePath();
        }

        // The path to the directory that contains the BUILD file.
        final Path absPackagePath;
        {
            absPackagePath = absDocumentPath.getParent();
        }

        // Don't allow autocompletions for anything other than BUILD files.
        if (!absDocumentPath.toString().endsWith("BUILD") && !absDocumentPath.toString().endsWith("BUILD.bazel")) {
            return completed(completions);
        }

        // Get the current file contents.
        final String currentFileContent;
        {
            final URI uri = URI.create(params.getTextDocument().getUri());
            currentFileContent = getTracker().getContents(uri);
        }

        // Get the current line being edited.
        final String currentLine;
        {
            final List<String> lines = Arrays.asList(currentFileContent.split("\n"));
            currentLine = lines.get(params.getPosition().getLine());
        }

        // Infer whether autocompletion should occur.
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

            // Try to infer if this completion should take place based on the starlark file.
            if (!getWizard().anyCallsContainPos(absDocumentPath.toUri(), params.getPosition())) {
                toAutocomplete = null;
                shouldAutocomplete = false;
            }
        }

        // Don't provide completions unless necessary/possible.
        {
            if (!shouldAutocomplete) {
                return completed(completions);
            }

            if (toAutocomplete.startsWith(TriggerCharacters.AT)) {
                return completed(completions);
            }
        }

        // Get the effective path of the directory to provide completions for. The varies
        // based on the trigger character.
        final Path rollingPath;
        boolean completeBuildTargets = false;
        {
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
                rollingPath = joinPaths(absPackagePath.toString(), localPath);
            } else {
                rollingPath = joinPaths(absPackagePath.toString(), toAutocomplete);
            }
        }

        // Verify that the path exists and that the path isn't a file (marking the
        // end of a completion).
        {
            if (!effectiveFileRepo().exists(rollingPath)) {
                return completed(completions);
            }

            if (effectiveFileRepo().isFile(rollingPath)) {
                return completed(completions);
            }
        }

        // Append all directories and files of a folder.
        {
            File directory = new File(rollingPath.toString());
            String[] children = Nullability.nullableOr(new String[]{}, directory::list);

            for (String child : children) {
                final CompletionItem item = new CompletionItem();
                item.setLabel(child);

                Path childPath = joinPaths(rollingPath.toString(), child);
                if (effectiveFileRepo().isDir(childPath)) {
                    item.setKind(CompletionItemKind.Folder);
                } else {
                    item.setKind(CompletionItemKind.File);
                }

                completions.add(item);
            }
        }

        // Append all inferred target names.
        final Path buildFile = getBuildFile(rollingPath);
        if (completeBuildTargets && buildFile != null) {
            final URI uri = buildFile.toUri();
            final ImmutableList<StarlarkWizard.TargetMeta> targets = getWizard().allDeclaredTargets(uri);
            for (StarlarkWizard.TargetMeta meta : targets) {
                final CompletionItem item = new CompletionItem();
                item.setLabel(meta.name().getValue());
                item.setKind(CompletionItemKind.Value);
                completions.add(item);
            }
        }

        return completed(completions);
    }

    private Path getBuildFile(Path pkgPath) {
        final Path buildBazelFile = joinPaths(pkgPath.toString(), "BUILD.bazel");
        final Path buildFile = joinPaths(pkgPath.toString(), "BUILD");

        if (effectiveFileRepo().exists(buildBazelFile)) {
            return buildBazelFile;
        }

        if (effectiveFileRepo().exists(buildFile)) {
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

    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> empty() {
        return completed(new ArrayList<>());
    }
}
