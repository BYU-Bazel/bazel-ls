package server.bazel.interp;

import com.google.common.base.Preconditions;

public class GraphEdge extends GraphComponent {
    final GraphNode start;
    final GraphNode end;

    private GraphEdge(UniqueID id, GraphNode start, GraphNode end) {
        super(id);
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);
        this.start = start;
        this.end = end;
    }

    public static GraphEdge fromStartEnd(GraphNode start, GraphNode end) {
        final UniqueID id = UniqueID.fromStartEndNodes(start, end);
        return new GraphEdge(id, start, end);
    }

    public GraphNode start() {
        return start;
    }

    public GraphNode end() {
        return end;
    }
}
