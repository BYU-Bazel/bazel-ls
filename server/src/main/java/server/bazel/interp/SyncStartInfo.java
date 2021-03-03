package server.bazel.interp;

public class SyncStartInfo {
    private final OldGraphNode startNode;

    private SyncStartInfo(OldGraphNode startNode) {
        this.startNode = startNode;
    }

    public OldGraphNode startNode() {
        return startNode;
    }

    public static class Builder {
        private OldGraphNode startNode;

        public Builder setStartNode(OldGraphNode startNode) {
            this.startNode = startNode;
            return this;
        }

        public SyncStartInfo build() {
            return new SyncStartInfo(startNode);
        }
    }
}
