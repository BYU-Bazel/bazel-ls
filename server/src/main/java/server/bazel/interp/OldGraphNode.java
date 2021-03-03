package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Observatory;

import java.util.HashMap;
import java.util.Map;

public final class OldGraphNode extends OldGraphComponent {
    private final Element element;
    private final Map<UniqueID, OldGraphEdge> inEdges;
    private final Map<UniqueID, OldGraphEdge> outEdges;
    private final Observatory<OldGraphNodeCallbacks> observatory;

    private OldGraphNode(UniqueID id, OldGraph graph, Element element) {
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

    public Observatory<OldGraphNodeCallbacks> observatory() {
        return observatory;
    }

    protected void onAttach() {

    }

    protected void onDetach() {

    }

    protected void sync(SyncContext context) {

    }

    public Iterable<OldGraphEdge> fowardDependencies() {
        return outEdges.values();
    }

    public boolean hasForwardDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.containsKey(id);
    }

    public OldGraphEdge getForwardDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.get(id);
    }

    public OldGraphEdge createForwardDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);

        if (hasForwardDependencyOn(other)) {
            return getForwardDependencyOn(other);
        }

        final OldGraphEdge edge = OldGraphEdge.fromStartEnd(this, other);
        this.outEdges.put(edge.id(), edge);
        other.inEdges.put(edge.id(), edge);

        this.observatory().notifyListeners((listener) -> listener.onForwardDependencyCreated(edge));
        other.observatory().notifyListeners((listener) -> listener.onReverseDependencyCreated(edge));

        return edge;
    }

    public OldGraphEdge removeFowardDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);

        if (!hasForwardDependencyOn(other)) {
            return null;
        }

        final OldGraphEdge edge = getForwardDependencyOn(other);
        this.outEdges.remove(edge.id());
        other.inEdges.remove(edge.id());

        this.observatory().notifyListeners((listener) -> listener.onForwardDependencyRemoved(edge));
        other.observatory().notifyListeners((listener) -> listener.onReverseDependencyRemoved(edge));

        return edge;
    }

    public Iterable<OldGraphEdge> reverseDependencies() {
        return inEdges.values();
    }

    public boolean hasReverseDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.hasForwardDependencyOn(this);
    }

    public OldGraphEdge getReverseDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.getForwardDependencyOn(this);
    }

    public OldGraphEdge createReverseDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.createForwardDependencyOn(this);
    }

    public OldGraphEdge removeReverseDependencyOn(OldGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.removeFowardDependencyOn(this);
    }
}
