package server.bazel.interp;

import server.utils.Context;

public class GraphNodeContext extends Context {
    private UniqueID creator;

    private GraphNodeContext() {
        super();
    }

    public static GraphNodeContext empty() {
        return new GraphNodeContext();
    }

    public boolean hasCreator() {
        return creator != null;
    }

    public UniqueID creator() {
        return creator;
    }

    public void setCreator(UniqueID creator) {
        this.creator = creator;
    }
}
