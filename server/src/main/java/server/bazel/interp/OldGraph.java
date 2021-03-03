package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.utils.Exceptions;

import java.util.HashMap;
import java.util.Map;

public class OldGraph {
    private static final Logger logger = LogManager.getLogger(OldGraph.class);

    private final OldGraphNode root;
    private final Map<UniqueID, OldGraphNode> nodes;

    private OldGraph() {
        root = new OldGraphNode<RootElement>();
        nodes = new HashMap<>();
    }

    public static OldGraph empty() {
        final OldGraph graph = new OldGraph();
        graph.nodes.put(graph.root.id(), graph.root);
        return graph;
    }

    public boolean containsFile(FileInfo info) throws GraphException {
        Preconditions.checkNotNull(info);
        return containsNode(info.uniqueID());
    }

    public void attachFile(FileInfo info) throws GraphException {
        Preconditions.checkNotNull(info);

        if (containsFile(info)) {
            throw new GraphException("File has already been attached.");
        }

        // Create a node to be added to the graph. This node will be a FileNode.
        FileElement node;
        switch (info.kind()) {
            case WORKSPACE:
                final WorkspaceElement.Builder builder = new WorkspaceElement.Builder();
                builder.setId(info.uniqueID());
                builder.setPath(info.path());
                node = builder.build();
                break;
            default:
                throw new Exceptions.Unimplemented();
        }

        // Add the node and consider the file to be "attached".
        addNode(node);

        // Sync with the file after adding to give some information to the node.
        syncFile(info);
    }

    public void syncFile(FileInfo info) throws GraphException {
        Preconditions.checkNotNull(info);

        if (!containsFile(info)) {
            throw new GraphException("Cannot sync with a file that has not been attached.");
        }

        // Get the node for the provided file information. We're guaranteed that this will be a file node.
        final FileElement node = getNode(info.uniqueID(), FileElement.class);
        assert node != null;

        // Construct a context in which to perform syncing.
        final SyncContext context = SyncContext.empty();
        {
            // Keep track of where the sync began.
            final SyncStartInfo.Builder startInfo = new SyncStartInfo.Builder();
            startInfo.setStartNode(node);
            context.put(SyncStartInfo.class, startInfo.build());
        }

        // Perform a sync on the node. This behavior is agnostic toward the current node. Each individual
        // node might have their own way of handling a sync.
        node.sync(context);
    }

    public void detachFile(FileInfo info) throws GraphException {
        Preconditions.checkNotNull(info);

        if (!containsFile(info)) {
            throw new GraphException("Cannot detach a file that has not been attached.");
        }

        throw new Exceptions.Unimplemented("TODO: Detach files should prune the graph.");
    }

    private boolean containsNode(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return nodes.containsKey(nodeID);
    }

    private OldGraphNode getNode(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return nodes.get(nodeID);
    }

    private <T extends OldGraphNode> T getNode(UniqueID nodeID, Class<T> clazz) {
        Preconditions.checkNotNull(nodeID);

        final OldGraphNode node = getNode(nodeID);
        if (node == null) {
            return null;
        }

        if (clazz.isInstance(node)) {
            return clazz.cast(node);
        }

        throw new ClassCastException(String.format("The desired node with id %s with class %s does not " +
                "inherit from the provided %s class.", nodeID, node.getClass(), clazz));
    }

    private void addNode(OldGraphNode node) throws GraphException {
        Preconditions.checkNotNull(node);
        if (containsNode(node.id())) {
            throw new GraphException("Node already exists.");
        }

        nodes.put(node.id(), node);
        root.createForwardDependencyOn(node);
    }

    private void removeNode(UniqueID nodeID) throws GraphException {
        Preconditions.checkNotNull(nodeID);
        if (!containsNode(nodeID)) {
            throw new GraphException("Node not found.");
        }

        final OldGraphNode node = getNode(nodeID);
        nodes.remove(nodeID);
        root.removeFowardDependencyOn(node);
    }
}
