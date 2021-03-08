package server.bazel.interp;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;

public abstract class Element implements GraphLifecycle {
    private static final Logger logger = LogManager.getLogger(Element.class);

    private GraphNode<?> node;
    private boolean initialized;

    protected Element() {
        super();
        this.node = null;
        this.initialized = false;
    }

    public abstract ElementKind kind();

    @Nonnull
    protected static Logger logger() {
        return logger;
    }

    public boolean initialized() {
        return initialized;
    }

    @Nonnull
    public GraphNode<?> node() {
        return node;
    }

    @Nonnull
    public Graph graph() {
        return node().graph();
    }

    public final void initialize(ElementArgs args) {
        Preconditions.checkNotNull(args);
        Preconditions.checkNotNull(args.getNode());

        if (initialized()) {
            throw new GraphRuntimeException("Element already initialized.");
        }

        initialized = true;
        node = args.getNode();
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onSync() {

    }

    @Override
    public void onFinish() {

    }
}
