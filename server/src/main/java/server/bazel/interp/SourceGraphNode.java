package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

public abstract class SourceGraphNode extends SourceGraphComponent {
    private final Map<UniqueID, SourceGraphEdge> inEdges;
    private final Map<UniqueID, SourceGraphEdge> outEdges;

    protected SourceGraphNode(UniqueID id) {
        super(id);
        this.inEdges = new HashMap<>();
        this.outEdges = new HashMap<>();
    }

    public abstract SourceGraphNodeKind nodeKind();

    Iterable<SourceGraphEdge> fowardDependencies() {
        return outEdges.values();
    }

    boolean hasForwardDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.containsKey(id);
    }

    SourceGraphEdge getForwardDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        final UniqueID id = UniqueID.fromStartEndNodes(this, other);
        return outEdges.get(id);
    }

    SourceGraphEdge createForwardDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);

        if (hasForwardDependencyOn(other)) {
            return getForwardDependencyOn(other);
        }

        final SourceGraphEdge edge = SourceGraphEdge.fromStartEnd(this, other);
        this.outEdges.put(edge.id(), edge);
        other.inEdges.put(edge.id(), edge);
        return edge;
    }

    SourceGraphEdge removeFowardDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);

        if (!hasForwardDependencyOn(other)) {
            return null;
        }

        final SourceGraphEdge edge = getForwardDependencyOn(other);
        this.outEdges.remove(edge.id());
        other.inEdges.remove(edge.id());
        return edge;
    }

    Iterable<SourceGraphEdge> reverseDependencies() {
        return inEdges.values();
    }

    boolean hasReverseDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.hasForwardDependencyOn(this);
    }

    SourceGraphEdge getReverseDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.getForwardDependencyOn(this);
    }

    SourceGraphEdge createReverseDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.createForwardDependencyOn(this);
    }

    SourceGraphEdge removeReverseDependencyOn(SourceGraphNode other) {
        Preconditions.checkNotNull(other);
        return other.removeFowardDependencyOn(this);
    }
}
