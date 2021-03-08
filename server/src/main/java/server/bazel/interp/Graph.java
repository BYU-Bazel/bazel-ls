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

    public UniqueID addFile(FileInfo info) {
        Preconditions.checkNotNull(info);

        final UniqueID id = UniqueID.fromPath(info.path());
        final GraphNode<?> node;
        switch (info.kind()) {
            case WORKSPACE:
                final WorkspaceElement element = new WorkspaceElement();
                node = new GraphNode<>(this, id, element);
                break;
            case BUILD:
            case BAZEL:
            default:
                throw new Exceptions.Unimplemented();
        }

        addNode(node);
        return id;
    }

    public boolean containsFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        final UniqueID id = UniqueID.fromPath(info.path());
        return containsNode(id);
    }

    public GraphNode<?> getFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        final UniqueID id = UniqueID.fromPath(info.path());
        return getNode(id);
    }

    public void syncFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        final UniqueID id = UniqueID.fromPath(info.path());
        syncNode(id);
    }

    public void removeFile(FileInfo info) {
        Preconditions.checkNotNull(info);
        final UniqueID id = UniqueID.fromPath(info.path());
        removeNode(id);
    }

    public GraphNode<?> getNode(UniqueID id) {
        Preconditions.checkNotNull(id);

        if (!containsNode(id)) {
            throw new GraphRuntimeException("Node not contained in graph.");
        }

        return nodes.get(id);
    }

    public boolean containsNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        return nodes.containsKey(id);
    }

    // TODO: add graph context.
    public void addNode(GraphNode<?> node) {
        Preconditions.checkNotNull(node);

        if (containsNode(node.id())) {
            throw new GraphRuntimeException("Node already contained in graph.");
        }

        nodes.put(node.id(), node);
        node.onStart();
        node.onSync();
    }

    // TODO: add graph context.
    public void syncNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        getNode(id).onSync();
    }

    // TODO: add graph context.
    public void removeNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        getNode(id).onSync();
        getNode(id).onFinish();
        nodes.remove(id);
    }
}
