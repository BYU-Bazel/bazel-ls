package server.bazel.interp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Element {
    private static final Logger logger = LogManager.getLogger(Element.class);

    protected Element() {
        super();
    }

    public abstract ElementKind elementKind();

    protected static Logger logger() {
        return logger;
    }
}
