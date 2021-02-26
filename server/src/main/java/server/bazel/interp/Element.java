package server.bazel.interp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Element {
    private static final Logger logger = LogManager.getLogger(Element.class);

    public abstract ElementKind kind();

    protected static Logger logger() {
        return logger;
    }
}
