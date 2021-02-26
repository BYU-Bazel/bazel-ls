package server.bazel.interp;

// TODO: Provide graph to all nodes
// TODO: Create node callbacks to automatically manage dependencies

public interface GraphNodeCallbacks {
    void onForwardDependencyCreated(GraphEdge dependency);

    void onForwardDependencyRemoved(GraphEdge dependency);

    void onReverseDependencyCreated(GraphEdge dependency);

    void onReverseDependencyRemoved(GraphEdge dependency);
}
