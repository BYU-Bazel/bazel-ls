package server.bazel.interp;

import com.google.common.base.Preconditions;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.utils.Exceptions;

import java.util.HashMap;
import java.util.Map;

public class SourceGraph {
    private static final Logger logger = LogManager.getLogger(SourceGraph.class);

    private final RootNode root;
    private final Map<UniqueID, SourceGraphNode> nodes;
    private final Map<UniqueID, SourceGraphEdge> edges;

    private SourceGraph() {
        root = new RootNode();
        nodes = new HashMap<>();
        edges = new HashMap<>();
    }

    public static SourceGraph empty() {
        final SourceGraph graph = new SourceGraph();
        graph.nodes.put(graph.root.id(), graph.root);
        return graph;
    }

    public boolean isFileAttached(FileInformation info) throws SourceGraphException {
        Preconditions.checkNotNull(info);
        return containsNode(info.uniqueID());
    }

    public void attachFile(FileInformation info) throws SourceGraphException {
        Preconditions.checkNotNull(info);

        if (isFileAttached(info)) {
            throw new SourceGraphException("File has already been attached.");
        }

        // Create a node to be added to the graph. This node will be a FileNode.
        FileNode node;
        final UniqueID id = info.uniqueID();
        switch (info.kind()) {
            case WORKSPACE:
                node = new WorkspaceFileNode(id);
                break;
            default:
                throw new Exceptions.Unimplemented();
        }

        // Add the node and consider the file to be "attached".
        addNode(node);

        // Sync with the file after adding to give some information to the node.
        syncFile(info);
    }

    public void syncFile(FileInformation info) throws SourceGraphException {
        Preconditions.checkNotNull(info);

        if (!isFileAttached(info)) {
            throw new SourceGraphException("Cannot sync with a file that has not been attached.");
        }

        // Get the node for the provided file information. We're guaranteed that this will be a file node.
        final FileNode node = getNode(info.uniqueID(), FileNode.class);
        assert node != null;

        // Construct a context in which to perform syncing.
        final SourceGraphSyncContext

        switch (node.fileKind()) {
            case WORKSPACE:
                break;
            case BAZEL:
            case BUILD:
                throw new Exceptions.Unimplemented();
        }
        // TODO:
        //  Switch over the file type. Start with workspace. If is workspace, then go through and
        //  constrcut nodes based on
        //  - Load statements (do these first just for testing)
        //  - Repository rules
        //  - Function calls which invoke things from the load statements
    }

    public void detachFile(FileInformation info) throws SourceGraphException {
        Preconditions.checkNotNull(info);

        if (!isFileAttached(info)) {
            throw new SourceGraphException("Cannot detach a file that has not been attached.");
        }

        throw new Exceptions.Unimplemented("TODO: Detach files should prune the graph.");
    }

    private boolean containsNode(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return nodes.containsKey(nodeID);
    }

    private SourceGraphNode getNode(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return nodes.get(nodeID);
    }

    private <T extends SourceGraphNode> T getNode(UniqueID nodeID, Class<T> clazz) {
        Preconditions.checkNotNull(nodeID);

        final SourceGraphNode node = getNode(nodeID);
        if (node == null) {
            return null;
        }

        if (clazz.isInstance(node)) {
            return clazz.cast(node);
        }

        throw new ClassCastException(String.format("The desired node with id %s with class %s does not " +
                "inherit from the provided %s class.", nodeID, node.getClass(), clazz));
    }

    private void addNode(SourceGraphNode node) throws SourceGraphException {
        Preconditions.checkNotNull(node);
        if (containsNode(node.id())) {
            throw new SourceGraphException("Node already exists.");
        }
        nodes.put(node.id(), node);
    }

    private void removeNode(UniqueID nodeID) throws SourceGraphException {
        Preconditions.checkNotNull(nodeID);
        if (!containsNode(nodeID)) {
            throw new SourceGraphException("Node not found.");
        }
        nodes.remove(nodeID);
    }
}
