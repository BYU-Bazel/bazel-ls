package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Callbacks;
import server.utils.Exceptions;

import java.util.HashMap;
import java.util.Map;

public class Graph {
    private final Map<UniqueID, GraphNode<?>> nodes;
    private final GraphContext context;
    private boolean running;

    private Graph() {
        super();
        this.nodes = new HashMap<>();
        this.context = GraphContext.empty();
        this.running = false;
    }

    public static Graph empty() {
        return new Graph();
    }

    public GraphContext context() {
        if (!running) {
            throw new GraphRuntimeException("No context active.");
        }

        return this.context;
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

    public void addNode(GraphNode<?> node) {
        Preconditions.checkNotNull(node);

        if (containsNode(node.id())) {
            throw new GraphRuntimeException("Node already contained in graph.");
        }

        nodes.put(node.id(), node);

//        wrapWithRunningContext((ctx) ->

        boolean isStartingPoint = !running;
        if (isStartingPoint) {
            this.running = true;
            context.clear();
        }
        node.onStart();
        if (isStartingPoint) {
            context.clear();
            this.running = false;
        }

        syncNode(node.id());
    }

    public void syncNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        getNode(id).onSync();
    }

    public void removeNode(UniqueID id) {
        Preconditions.checkNotNull(id);

        syncNode(id);

        getNode(id).onFinish();
        nodes.remove(id);
    }

    private void wrapWithRunningContext(
            Callbacks.Consumer<GraphContext> initContextCallback,
            Callbacks.Event runCallback
    ) {
        Preconditions.checkNotNull(initContextCallback);
        Preconditions.checkNotNull(runCallback);

        boolean isStartingPoint = !running;
        if (isStartingPoint) {
            this.running = true;
            context().clear();
            initContextCallback.invoke(context());
        }

        runCallback.invoke();

        if (isStartingPoint) {
            context().clear();
            this.running = false;
        }
    }
}
