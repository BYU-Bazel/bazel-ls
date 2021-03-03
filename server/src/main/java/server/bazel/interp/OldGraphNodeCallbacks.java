package server.bazel.interp;

// TODO: Provide graph to all nodes
// TODO: Create node callbacks to automatically manage dependencies

public interface OldGraphNodeCallbacks {
    void onForwardDependencyCreated(OldGraphEdge dependency);

    void onForwardDependencyRemoved(OldGraphEdge dependency);

    void onReverseDependencyCreated(OldGraphEdge dependency);

    void onReverseDependencyRemoved(OldGraphEdge dependency);
}
