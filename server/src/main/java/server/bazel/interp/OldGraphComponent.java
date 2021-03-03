package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class OldGraphComponent {
    private static final Logger logger = LogManager.getLogger(OldGraphComponent.class);

    private final UniqueID id;
    private final OldGraph graph;

    OldGraphComponent(UniqueID id, OldGraph graph) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(graph);
        this.id = id;
        this.graph = graph;
    }

    public UniqueID id() {
        return id;
    }

    public OldGraph graph() {
        return graph;
    }

    protected static Logger logger() {
        return logger;
    }
}
