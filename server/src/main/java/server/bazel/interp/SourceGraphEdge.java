package server.bazel.interp;

import com.google.common.base.Preconditions;

public class SourceGraphEdge extends SourceGraphComponent {
    final SourceGraphNode start;
    final SourceGraphNode end;

    private SourceGraphEdge(UniqueID id, SourceGraphNode start, SourceGraphNode end) {
        super(id);
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);
        this.start = start;
        this.end = end;
    }

    public static SourceGraphEdge fromStartEnd(SourceGraphNode start, SourceGraphNode end) {
        final UniqueID id = UniqueID.fromStartEndNodes(start, end);
        return new SourceGraphEdge(id, start, end);
    }

    public SourceGraphNode start() {
        return start;
    }

    public SourceGraphNode end() {
        return end;
    }
}
