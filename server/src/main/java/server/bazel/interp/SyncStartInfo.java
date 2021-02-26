package server.bazel.interp;

public class SyncStartInfo {
    private final GraphNode startNode;

    private SyncStartInfo(GraphNode startNode) {
        this.startNode = startNode;
    }

    public GraphNode startNode() {
        return startNode;
    }

    public static class Builder {
        private GraphNode startNode;

        public Builder setStartNode(GraphNode startNode) {
            this.startNode = startNode;
            return this;
        }

        public SyncStartInfo build() {
            return new SyncStartInfo(startNode);
        }
    }
}
