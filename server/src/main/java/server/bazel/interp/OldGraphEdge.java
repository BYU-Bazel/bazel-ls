package server.bazel.interp;

import com.google.common.base.Preconditions;

public class OldGraphEdge extends OldGraphComponent {
    final OldGraphNode start;
    final OldGraphNode end;

    private OldGraphEdge(UniqueID id, OldGraphNode start, OldGraphNode end) {
        super(id);
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);
        this.start = start;
        this.end = end;
    }

    public static OldGraphEdge fromStartEnd(OldGraphNode start, OldGraphNode end) {
        final UniqueID id = UniqueID.fromStartEndNodes(start, end);
        return new OldGraphEdge(id, start, end);
    }

    public OldGraphNode start() {
        return start;
    }

    public OldGraphNode end() {
        return end;
    }
}
