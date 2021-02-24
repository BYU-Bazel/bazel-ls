package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.utils.Exceptions;

import java.nio.file.Path;
import java.util.*;

public class SourceGraph {
    private static final Logger logger = LogManager.getLogger(SourceGraph.class);

    private final RootNode root;
    private final Map<UniqueID, SourceGraphNode> nodes;

    private SourceGraph() {
        root = new RootNode();

        // The root node should always be present in the graph.
        nodes = new HashMap<>();
        nodes.put(root.id(), root);
    }

    public static SourceGraph empty() {
        return new SourceGraph();
    }

    public void syncWithWorkspaceFile(FileInformation file) throws SourceGraphException {
        Preconditions.checkNotNull(file);
        Preconditions.checkArgument(file.kind() == FileKind.WORKSPACE);
    }

    // TODO: Improve/fix
    public boolean containsNode(UniqueID id) {
        return nodes.containsKey(id);
    }

    // TODO: Improve/fix
    public SourceGraphNode getNode(UniqueID id) {
        if (!containsNode(id)) {
            return null;
        }

        return nodes.get(id);
    }

    // TODO: Improve/fix
    public UniqueID addFileNode(Path path) throws SourceGraphException {
        Preconditions.checkNotNull(path);

        final UniqueID id = UniqueID.fromPath(path);
        if (containsNode(id)) {
            throw new SourceGraphException("Node already contained in source graph.");
        }

        FileNode fileNode;
        final FileKind kind = InterpUtility.inferFileKind(path);
        switch (kind) {
            case WORKSPACE:
                fileNode = new WorkspaceFileNode(id);
                break;
            default:
                throw new Exceptions.Unimplemented();
        }

        nodes.put(id, fileNode);
        return id;
    }

    // TODO: Improve/fix
    public void removeFileNode(Path path) throws SourceGraphException {
        Preconditions.checkNotNull(path);

        final UniqueID id = UniqueID.fromPath(path);
        if (!containsNode(id)) {
            throw new SourceGraphException("Node not contained in source graph.");
        }

        nodes.remove(id);
    }
}
