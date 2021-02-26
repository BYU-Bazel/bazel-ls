package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.utils.Observatory;

import java.util.HashMap;
import java.util.Map;

public final class GraphNode extends GraphComponent {
    private final Element element;
    private final Map<UniqueID, GraphEdge> inEdges;
    private final Map<UniqueID, GraphEdge> outEdges;
    private final Observatory<GraphNodeCallbacks> observatory;

    private GraphNode(UniqueID id, Graph graph, Element element) {
        super(id, graph);

        Preconditions.checkNotNull(element);
        this.element = element;
        this.inEdges = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.observatory = new Observatory<>();
    }

    public Element element() {
        return element;
    }

    public <T extends Element> Element elementAs(Class<T> clazz) {
        if (clazz.isInstance(element())) {
            return clazz.cast(element());
        }

        throw new ClassCastException();
    }

    public Observatory<GraphNodeCallbacks> observatory() {
        return observatory;
    }

    protected void onAttach() {

    }

    protected void onDetach() {

    }

    protected void sync(SyncContext context) {

    }

    public Iterable<GraphEdge> fowardDependencies() {
        return outEdges.values();
    }

    public boolean hasForwardDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.containsKey(id);
    }

    public GraphEdge getForwardDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.get(id);
    }

    public GraphEdge createForwardDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);

        if (hasForwardDependencyOn(other)) {
            return getForwardDependencyOn(other);
        }

        final GraphEdge edge = GraphEdge.fromStartEnd(this, other);
        this.outEdges.put(edge.id(), edge);
        other.inEdges.put(edge.id(), edge);

        this.observatory().notifyListeners((listener) -> listener.onForwardDependencyCreated(edge));
        other.observatory().notifyListeners((listener) -> listener.onReverseDependencyCreated(edge));

        return edge;
    }

    public GraphEdge removeFowardDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);

        if (!hasForwardDependencyOn(other)) {
            return null;
        }

        final GraphEdge edge = getForwardDependencyOn(other);
        this.outEdges.remove(edge.id());
        other.inEdges.remove(edge.id());

        this.observatory().notifyListeners((listener) -> listener.onForwardDependencyRemoved(edge));
        other.observatory().notifyListeners((listener) -> listener.onReverseDependencyRemoved(edge));

        return edge;
    }

    public Iterable<GraphEdge> reverseDependencies() {
        return inEdges.values();
    }

    public boolean hasReverseDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        return other.hasForwardDependencyOn(this);
    }

    public GraphEdge getReverseDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        return other.getForwardDependencyOn(this);
    }

    public GraphEdge createReverseDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        return other.createForwardDependencyOn(this);
    }

    public GraphEdge removeReverseDependencyOn(GraphNode other) {
        Preconditions.checkNotNull(other);
        return other.removeFowardDependencyOn(this);
    }
}
