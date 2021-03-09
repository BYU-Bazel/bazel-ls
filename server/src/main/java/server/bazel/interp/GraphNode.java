package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

public final class GraphNode<T extends Element> implements GraphLifecycle {
    private final Graph graph;
    private final UniqueID id;
    private final T element;
    private final Map<UniqueID, GraphNode<?>> forwardEdges;
    private final Map<UniqueID, GraphNode<?>> reverseEdges;

    public GraphNode(Graph graph, UniqueID id, T element) {
        super();
        Preconditions.checkNotNull(graph);
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(element);
        this.graph = graph;
        this.id = id;
        this.element = element;
        this.forwardEdges = new HashMap<>();
        this.reverseEdges = new HashMap<>();
    }

    public Graph graph() {
        return graph;
    }

    public GraphContext context() {
        return graph().context();
    }

    public UniqueID id() {
        return id;
    }

    public T element() {
        return element;
    }

    public <G extends Element> G elementAs(Class<G> type) {
        Preconditions.checkNotNull(type);
        if (type.isInstance(element())) {
            return type.cast(element());
        }
        throw new ClassCastException();
    }

    public Iterable<GraphNode<?>> forwardEdges() {
        return forwardEdges.values();
    }

    public boolean containsForwardEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return forwardEdges.containsKey(nodeID);
    }

    public GraphNode<?> getForwardEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return forwardEdges.get(nodeID);
    }

    public void addForwardEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);

        if (nodeID.equals(id())) {
            throw new GraphRuntimeException("A node cannot depend on itself.");
        }

        if (!graph().containsNode(nodeID)) {
            throw new GraphRuntimeException("Dependent nodes must share the same graph.");
        }

        if (containsForwardEdge(nodeID)) {
            throw new GraphRuntimeException("Dependency already added.");
        }

        final GraphNode<?> other = graph().getNode(nodeID);
        this.forwardEdges.put(other.id(), other);
        other.reverseEdges.put(this.id(), this);
    }

    public void removeForwardEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);

        if (!containsForwardEdge(nodeID)) {
            throw new GraphRuntimeException("Dependency does not exist.");
        }

        final GraphNode<?> other = graph().getNode(nodeID);
        this.forwardEdges.remove(other.id());
        other.reverseEdges.remove(this.id());
    }

    public Iterable<GraphNode<?>> reverseEdges() {
        return reverseEdges.values();
    }

    public boolean containsReverseEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return reverseEdges.containsKey(nodeID);
    }

    public GraphNode<?> getReverseEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);
        return reverseEdges.get(nodeID);
    }

    public void addReverseEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);

        if (!graph().containsNode(nodeID)) {
            throw new GraphRuntimeException("Dependent nodes must share the same graph.");
        }

        // Adding a reverse edge is the same as adding a forward edge on the target node.
        final GraphNode<?> other = graph().getNode(nodeID);
        other.addForwardEdge(id());
    }

    public void removeReverseEdge(UniqueID nodeID) {
        Preconditions.checkNotNull(nodeID);

        if (!graph().containsNode(nodeID)) {
            throw new GraphRuntimeException("Dependent nodes must share the same graph.");
        }

        // Removing a reverse edge is the same as removing a forward edge on the target node.
        final GraphNode<?> other = graph().getNode(nodeID);
        other.removeReverseEdge(id());
    }

    @Override
    public void onStart() {
        final ElementArgs args = new ElementArgs();
        args.setNode(this);
        element().initialize(args);
        element().onStart();
    }

    @Override
    public void onSync() {
        element().onSync();
    }

    @Override
    public void onFinish() {
        element().onFinish();
    }
}
