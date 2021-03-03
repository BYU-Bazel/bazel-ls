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

        OldGraph graph = OldGraph.empty();
        {
            final FileInfo workspaceInfo = FileInfo.fromPath(workspacePath);

            logger.info("Building graph using workspace file.");
            logger.info("Does graph contain [" + workspacePath + "] path: " +
                    graph.containsFile(workspaceInfo));

            logger.info("Attaching path [" + workspacePath + "] to source graph.");
            graph.attachFile(workspaceInfo);

            logger.info("Does graph contain [" + workspacePath + "] path: " +
                    graph.containsFile(workspaceInfo));
        }
    }
}
