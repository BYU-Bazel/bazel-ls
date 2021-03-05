package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.HashSet;

public final class GraphNode<T extends Element> {
    private final UniqueID id;
    private final T element;
    private final GraphNodeContext context;
    private final HashSet<GraphNode<?>> forwardEdges;
    private final HashSet<GraphNode<?>> reverseEdges;

    public GraphNode(UniqueID id, T element) {
        super();
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(element);
        this.id = id;
        this.element = element;
        this.context = GraphNodeContext.empty();
        this.forwardEdges = new HashSet<>();
        this.reverseEdges = new HashSet<>();
    }

    //
//    public static <E extends Element> GraphNode<E> fromParams(Params<E> params) {
//        final GraphNode<E> result = new GraphNode<E>(params.getElement());
//
//        if (params.getDeclaringNodeId() != null) {
//            result.context
//        }
//
//        return result;
//    }

    public UniqueID id() {
        return id;
    }

    public T element() {
        return element;
    }

    public GraphNodeContext context() {
        return context;
    }

    public HashSet<GraphNode<?>> forwardEdges() {
        return forwardEdges;
    }

    public HashSet<GraphNode<?>> reverseEdges() {
        return reverseEdges;
    }

    protected void start() {
        element().onStart();
    }

    protected void sync() {
        element().onSync();
    }

    protected void finish() {
        element().onFinish();
    }
}
