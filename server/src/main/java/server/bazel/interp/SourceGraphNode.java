package server.bazel.interp;

import com.google.common.base.Preconditions;

public abstract class SourceGraphNode {
    private final UniqueID id;

    SourceGraphNode(UniqueID id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    public UniqueID id() {
        return id;
    }
}
