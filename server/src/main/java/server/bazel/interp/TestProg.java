package server.bazel.interp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestProg {
    private static final Logger logger = LogManager.getLogger(TestProg.class);

    public static void main(String[] args) throws Exception {
        logger.info("Hello world");

        // This path only works when running in dev environment.
        Path workspacePath = Paths.get("/workspaces/bazel-ls/WORKSPACE");
        String workspaceContent = Files.readString(workspacePath);
        logger.info("Workspace content:\n" + workspaceContent.substring(0, 150) + "...");

        SourceGraph sourceGraph = SourceGraph.empty();
        {
            final FileInformation workspaceInfo = FileInformation.fromPath(workspacePath);

            logger.info("Building graph using workspace file.");
            logger.info("Does graph contain [" + workspacePath + "] path: " +
                    sourceGraph.isFileAttached(workspaceInfo));

            logger.info("Attaching path [" + workspacePath + "] to source graph.");
            sourceGraph.attachFile(workspaceInfo);

            logger.info("Does graph contain [" + workspacePath + "] path: " +
                    sourceGraph.isFileAttached(workspaceInfo));
        }
    }
}
