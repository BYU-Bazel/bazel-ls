package server.codelens;

import java.util.concurrent.CompletableFuture;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import server.utils.DocumentTracker;
import server.workspace.Workspace;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPI;
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

<<<<<<< HEAD
        URI uri = new URI(params.getTextDocument().getUri());
        List<BuildTarget> targets = findTargets(uri);
        String contents = documentTracker.getContents(uri);
=======
        String contents = "";
        try {
            contents = documentTracker.getContents(new URI(params.getTextDocument().getUri()));
        } catch (Exception e) {
            logger.error(e);
        }
>>>>>>> 0c036fd32503ad72bea5e3b73cad1693630f0354

        List<CodeLens> results = new ArrayList<>();

        for (BuildTarget target : targets) {
            Codelens result = new CodeLens();
            result.setRange(findRangeForTarget(target, contents));
            result.setCommand(findCommandForTarget(target));
            results.add(result);
        }

<<<<<<< HEAD
        // CodeLens dummy = new CodeLens();
        // Range range = new Range(new Position(4, 0), new Position(4, 13));
        // Command command = new Command();
        // command.setTitle("Do Nothing");
        // command.setCommand(CommandConstants.none);
        // dummy.setRange(range);
        // dummy.setCommand(command);

        return CompletableFuture.completedFuture(results);
    }
=======
>>>>>>> 0c036fd32503ad72bea5e3b73cad1693630f0354

    private List<BuildTarget> findTargets(URI uri) {

        final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
        final WorkspaceAPI api = new WorkspaceAPI(tree);

        Path path = Path.of(uri);
        List<BuildTarget> targets = api.findPossibleTargetsForPath(path.getParent());

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

    private Command getCommandForTarget(BuildTarget target) {
        Command command = new Command();
        command.setTitle("Do Nothing to " + target.getLabel());
        command.setCommand(CommandConstants.none);
        return command;
    }

}