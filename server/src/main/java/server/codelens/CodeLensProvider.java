package server.codelens;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
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

/**
 * This class is delegated the CodeLens functionality of the BazelServices class
 */
public class CodeLensProvider {

    private static final Logger logger = LogManager.getLogger(CodeLensProvider.class);

    private DocumentTracker documentTracker;

    /**
     * Public constructor for CodeLensProvider
     * 
     * @param documentTracker DocumentTracker used to extract the contents of a text document
     */
    public CodeLensProvider(DocumentTracker documentTracker) {
        this.documentTracker = documentTracker;
    }

    /**
     * Parses a document for actionable BUILD targets and creates codelens objects corresponding to them
     * 
     * @param params the information passed to the server from the client containing the document to retrieve codelens for
     * @return a list of CodeLens objects representing the CodeLens options to be displayed to the user
     */
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
            if(target.getKind().contains("_binary") || target.getKind().contains("_test")) {
                CodeLens result = new CodeLens();
                Range range = findRangeForTarget(target, contents);
                if(range == null) continue;
                result.setRange(range);
                result.setCommand(findCommandForTarget(target, path));
                results.add(result);
            }
        }
        return CompletableFuture.completedFuture(results);
    }

    /**
     * Uses the workspace tree to parse the document for BUILD targets
     * 
     * @param path the path to the directory of the BUILD file
     * @return a list of BUILD targets
     */
    private List<BuildTarget> findTargets(String path) {

        final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
        final WorkspaceAPI api = new WorkspaceAPI(tree);

        List<BuildTarget> targets = null;
        try {
            targets = api.findPossibleTargetsForPath(Paths.get(path));
        }
        catch(WorkspaceAPIException e) {
            logger.error(e + " in: " + path);
        }

        return targets;
    }

    /**
     * Locates the declaration of a BUILD target within a BUILD file
     * 
     * @param target the BUILD target to find
     * @param contents a String of the document's contents
     * @return a Range spanning the declaration of the BUILD target
     */
    private Range findRangeForTarget(BuildTarget target, String contents) {

        String nameDeclaration = "name = \"" + target.getLabel() + "\"";
        int nameIndex = contents.indexOf(nameDeclaration);
        if(nameIndex == -1) return null;
        int kindIndex = contents.substring(0, nameIndex).lastIndexOf(target.getKind());
        if (kindIndex < 0) {
            String altIndicator = target.getKind().substring(target.getKind().lastIndexOf("_"), target.getKind().length());
            kindIndex = contents.substring(0, nameIndex).lastIndexOf(altIndicator);
        }
        int kindEndIndex = kindIndex + target.getKind().length();
        int lineNumber = 0;
        for (int i = 0; i < kindIndex; i++) {
            if (contents.charAt(i) == '\n') {
                lineNumber++;
            }
        }

        return new Range(new Position(lineNumber, 0), new Position(lineNumber, kindEndIndex - kindIndex));

    }

    /**
     * Generates command parameters that the client can return to the server if the user selects the relevant codelens
     * 
     * @param target the BUILD target that the command should act on
     * @param path the path to the directory containing the BUILD file
     * @return a Command that the server can interpret to act on an actionable BUILD target
     */
    private Command findCommandForTarget(BuildTarget target, String path) {
        Command command = new Command();
        List<Object> args = new ArrayList<Object>();
        if(target.getKind().contains("_binary")) {
            command.setTitle("Build " + target.getLabel());
            command.setCommand(AllCommands.build);
            logger.info("Setting command " + AllCommands.build + " with path arg of " + path + ":" + target.getLabel());
        } else if (target.getKind().contains("_test")) {
            command.setTitle("Test " + target.getLabel());
            command.setCommand(AllCommands.test);
            logger.info("Setting command " + AllCommands.test + " with a path arg of " + path + ":" + target.getLabel());
        }
        args.add(path + ":" + target.getLabel());
        command.setArguments(args);
        
        return command;
    }

    /**
     * Generates the path from the WORKSPACE root to the directory containing the BUILD file
     * @param uri the URI of the BUILD file
     * @return the path from the WORKSPACE root to the directory containing the BUILD file
     */
    private String buildPath(URI uri) {
        File file = new File(uri);
        StringBuilder pathBuilder = new StringBuilder();
        boolean rootPath = true;
        while(file != null) {
            if(file.isDirectory()) {
                if(Arrays.asList(file.list()).contains("WORKSPACE") || Arrays.asList(file.list()).contains("WORKSPACE.bzl")) {
                    if (rootPath) {
                        pathBuilder.insert(0, "/");
                    }
                    break;
                }
                else {
                    pathBuilder.insert(0, "/" + file.getName());
                    rootPath = false;
                }
            }
            file = new File(file.getParent());
        }
        pathBuilder.insert(0, "/");
        return pathBuilder.toString();
    }
}
