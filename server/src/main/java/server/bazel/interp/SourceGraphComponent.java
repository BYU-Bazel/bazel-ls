package server.bazel.interp;

import com.google.common.base.Preconditions;

public abstract class SourceGraphComponent {
    final UniqueID id;

    protected SourceGraphComponent(UniqueID id) {
        Preconditions.checkNotNull(id);
        this.id = id;
    }

    public UniqueID id() {
        return id;
    }
}
