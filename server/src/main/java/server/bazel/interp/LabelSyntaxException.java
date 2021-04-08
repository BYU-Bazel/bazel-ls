package server.bazel.interp;

import server.bazel.interp.InterpException;

public class LabelSyntaxException extends InterpException {
    public LabelSyntaxException() {
        super();
    }

    public LabelSyntaxException(String msg) {
        super(msg);
    }
}
