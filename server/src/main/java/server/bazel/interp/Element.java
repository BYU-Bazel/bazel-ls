package server.bazel.interp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TODO:
 * Element has to have a referene to the node
 * Element has to be created
 * Element should have a getter for the graph and the node, graph is referenced through the ndoe
 * Node should have a reference to the graph
 * Node should have a big constructor that passes in: Element, NodeID, and Graph
 */
public abstract class Element {
    private static final Logger logger = LogManager.getLogger(Element.class);

    protected Element() {
        super();
    }

    public abstract ElementKind kind();

    protected static Logger logger() {
        return logger;
    }

    protected void onStart() {

    }

    protected void onSync() {

    }

    protected void onFinish() {

    }
}
