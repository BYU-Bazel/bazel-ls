package server.bazel.interp;

public final class ElementArgs {
    private GraphNode<?> node;

    public GraphNode<?> getNode() {
        return node;
    }

    public void setNode(GraphNode<?> node) {
        this.node = node;
    }
}
