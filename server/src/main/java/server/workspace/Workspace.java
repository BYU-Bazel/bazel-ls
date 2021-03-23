package server.workspace;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.bazel.cli.Bazel;
import server.bazel.cli.BazelServerException;
import server.bazel.tree.BuildTarget;
import server.bazel.tree.Package;
import server.bazel.tree.SourceFile;
import server.bazel.tree.WorkspaceTree;

import java.util.*;

public class Workspace {
    private static final Logger logger = LogManager.getLogger(Workspace.class);
    private static final Workspace instance = new Workspace();

    private ExtensionConfig extensionConfig;
    private ProjectFolder rootFolder;
    private Set<ProjectFolder> workspaceFolders;
    private WorkspaceTree workspaceTree;

    private Workspace() {
        extensionConfig = null;
        rootFolder = null;
        workspaceFolders = new HashSet<>();
        workspaceTree = initialWsTree();
    }

    public static Workspace getInstance() {
        return instance;
    }

    public WorkspaceTree getWorkspaceTree() {
        return workspaceTree;
    }

    public ExtensionConfig getExtensionConfig() {
        return extensionConfig;
    }

    public void setExtensionConfig(ExtensionConfig extensionConfig) {
        this.extensionConfig = extensionConfig;
    }

    public ProjectFolder getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(ProjectFolder rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Iterable<ProjectFolder> getWorkspaceFolders() {
        return workspaceFolders;
    }

    public void addWorkspaceFolders(Collection<ProjectFolder> folders) {
        Preconditions.checkNotNull(folders);
        workspaceFolders.addAll(folders);
    }

    public void removeWorkspaceFolders(Collection<ProjectFolder> folders) {
        Preconditions.checkNotNull(folders);
        workspaceFolders.removeAll(folders);
    }

    private static WorkspaceTree initialWsTree() {
        return new WorkspaceTree(new Package("/"));
    }

    /**
     * Syncs the workspace tree with all files in memory.
     *
     * @throws BazelServerException If something fails.
     */
    public void syncWorkspace() throws BazelServerException {
        workspaceTree = initialWsTree();

        List<BuildTarget> buildTargets;
        List<SourceFile> sourceFiles;
        try {
            buildTargets = getBuildTargets();
            sourceFiles = getSourceFiles();
            buildTargets.forEach(this::addTargetToTree);
            sourceFiles.forEach(this::addSourceToTree);
        } catch (BazelServerException e) {
            logger.info(e.getMessage());
            throw e;
        }
    }

    public List<SourceFile> getSourceFiles() throws BazelServerException {
        return Bazel.getSourceFiles();
    }

    public List<BuildTarget> getBuildTargets() throws BazelServerException {
        return Bazel.getBuildTargets();
    }

    private void addTargetToTree(BuildTarget target) {
        logger.info("Adding target: " + target.toString());
        String[] pathParts = target.getPath().toString().split("/");
        WorkspaceTree.Node node = workspaceTree.getRoot();
        for (String part : pathParts) {
            if (!part.isEmpty()) {
                Optional<WorkspaceTree.Node> child = node.getChild(part);
                if (child.isPresent()) {
                    node = child.get();
                } else {
                    WorkspaceTree.Node childNode = node.addChild(new Package(part));
                    node = childNode;
                }
            }
        }
        node.getValue().addBuildTarget(target);
        logger.info("Post add target: " + node.getValue().getBuildTargets());
    }

    private void addSourceToTree(SourceFile source) {
        logger.info("Adding source file: " + source.toString());
        String[] pathParts = source.getPath().toString().split("/");
        WorkspaceTree.Node node = workspaceTree.getRoot();
        for (String part : pathParts) {
            if (!part.isEmpty()) {
                Optional<WorkspaceTree.Node> child = node.getChild(part);
                if (child.isPresent()) {
                    node = child.get();
                } else {
                    WorkspaceTree.Node childNode = node.addChild(new Package(part));
                    node = childNode;
                }
            }
        }
        node.getValue().addSourceFile(source);
        logger.info("Post add source file: " + node.getValue().getSourceFiles());
    }
}
