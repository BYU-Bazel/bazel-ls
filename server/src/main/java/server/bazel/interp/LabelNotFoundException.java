package server.bazel.interp;

import server.bazel.interp.InterpException;

public class LabelNotFoundException extends InterpException {
    public LabelNotFoundException() {
        super();
    }

    public LabelNotFoundException(String msg) {
        super(msg);
    }
}
