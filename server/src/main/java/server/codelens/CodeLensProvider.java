package server.codelens;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import server.commands.AllCommands;
import server.utils.DocumentTracker;
import server.workspace.Workspace;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPI;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPIException;
import server.bazel.tree.BuildTarget;
import server.bazel.tree.WorkspaceTree;

public class CodeLensProvider {

    private static final Logger logger = LogManager.getLogger(CodeLensProvider.class);

    private DocumentTracker documentTracker;

    public CodeLensProvider(DocumentTracker documentTracker) {
        this.documentTracker = documentTracker;
    }

    public CompletableFuture<List<? extends CodeLens>> getCodeLens(CodeLensParams params) {
        logger.info("CodeLens Provider invoked");

        URI uri = null;
        try {
            uri = new URI(params.getTextDocument().getUri());
        }
        catch(Exception e) {
            logger.error("Unable to get URI: " + e);
        }
        String path = buildPath(uri);
        List<BuildTarget> targets = findTargets(path);
        String contents = documentTracker.getContents(uri);

        List<CodeLens> results = new ArrayList<>();

        for (BuildTarget target : targets) {
            CodeLens result = new CodeLens();
            result.setRange(findRangeForTarget(target, contents));
            result.setCommand(findCommandForTarget(target, path));
            results.add(result);
        }

        return CompletableFuture.completedFuture(results);
    }

    private List<BuildTarget> findTargets(String path) {

        final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
        final WorkspaceAPI api = new WorkspaceAPI(tree);

        List<BuildTarget> targets = null;
        try {
            targets = api.findPossibleTargetsForPath(Path.of(path));
        }
        catch(WorkspaceAPIException e) {
            logger.error(e + " in: " + path);
        }

        return targets;
    }

    private Range findRangeForTarget(BuildTarget target, String contents) {

        String nameDeclaration = "name = \"" + target.getLabel() + "\"";
        int nameIndex = contents.indexOf(nameDeclaration);
        int kindIndex = contents.substring(0, nameIndex).lastIndexOf(target.getKind());
        int kindEndIndex = kindIndex + target.getKind().length();
        int lineNumber = 0;
        for (int i = 0; i < kindIndex; i++) {
            if (contents.charAt(i) == '\n') {
                lineNumber++;
            }
        }

        return new Range(new Position(lineNumber, kindIndex), new Position(lineNumber, kindEndIndex));

    }

    private Command findCommandForTarget(BuildTarget target, String path) {
        Command command = new Command();
        command.setTitle("Build " + target.getLabel());
        command.setCommand(AllCommands.build);
        List<Object> args = new ArrayList<Object>();
        logger.info("Setting command " + AllCommands.build + " with path arg of " + path + ":" + target.getLabel());
        args.add(path + ":" + target.getLabel());
        command.setArguments(args);
        return command;
    }

    private String buildPath(URI uri) {
        File file = new File(uri);
        StringBuilder pathBuilder = new StringBuilder();
        while(file != null) {
            if(file.isDirectory()) {
                if(Arrays.asList(file.list()).contains("WORKSPACE") || Arrays.asList(file.list()).contains("WORKSPACE.bzl")) {
                    pathBuilder.insert(0, "/");
                    break;
                }
                else {
                    pathBuilder.insert(0, "/" + file.getName());
                }
            }
            file = new File(file.getParent());
        }
        return pathBuilder.toString();
    }
}
