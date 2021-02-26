package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class GraphComponent {
    private static final Logger logger = LogManager.getLogger(GraphComponent.class);

    private final UniqueID id;
    private final Graph graph;

    GraphComponent(UniqueID id, Graph graph) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(graph);
        this.id = id;
        this.graph = graph;
    }

    public UniqueID id() {
        return id;
    }

    public Graph graph() {
        return graph;
    }

    protected static Logger logger() {
        return logger;
    }
}
