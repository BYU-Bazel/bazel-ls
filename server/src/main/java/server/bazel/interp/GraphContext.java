package server.bazel.interp;

import server.utils.Context;

public class GraphContext extends Context {
    private UniqueID creator;

    private GraphContext() {
        super();
    }

    public static GraphContext empty() {
        return new GraphContext();
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
