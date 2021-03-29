package server.bazel.interp;

public class LabelNotFoundException extends InterpException {
    public LabelNotFoundException() {
        super();
    }

    public LabelNotFoundException(String msg) {
        super(msg);
    }
}
