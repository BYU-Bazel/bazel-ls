package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Exceptions;

import java.util.HashMap;
import java.util.Map;

public class Graph {
    private final Map<UniqueID, GraphNode<?>> nodes;

    private Graph() {
        super();
        this.nodes = new HashMap<>();
    }

    public static Graph empty() {
        return new Graph();
    }

    public UniqueID addWorkspaceFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        Preconditions.checkArgument(info.kind() == FileKind.WORKSPACE);

        final UniqueID id = UniqueID.fromPath(info.path());
        final WorkspaceElement element = new WorkspaceElement();
        final GraphNode<WorkspaceElement> node = new GraphNode<>(id, element);

        addNode(node);
        return id;
    }

    public void syncWorkspaceFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        Preconditions.checkArgument(info.kind() == FileKind.WORKSPACE);

        final UniqueID id = UniqueID.fromPath(info.path());
        syncNode(id);
    }

    public void removeWorkspaceFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        Preconditions.checkArgument(info.kind() == FileKind.WORKSPACE);

        final UniqueID id = UniqueID.fromPath(info.path());
        removeNode(id);
    }

    public GraphNode<?> getNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        return nodes.get(id);
    }

    public boolean containsNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        return nodes.containsKey(id);
    }

    public void addNode(GraphNode<?> node) {
        Preconditions.checkNotNull(node);
        nodes.put(node.id(), node);
        node.start();
        node.sync();
    }

    public void syncNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        getNode(id).sync();
    }

    public void removeNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        getNode(id).sync();
        getNode(id).finish();
        nodes.remove(id);
    }
}
