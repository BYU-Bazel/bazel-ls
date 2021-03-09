package server.bazel.interp;

import server.utils.Context;

public class GraphContext extends Context {
    private GraphContext() {
        super();
    }

    public static GraphContext empty() {
        return new GraphContext();
    }
}
