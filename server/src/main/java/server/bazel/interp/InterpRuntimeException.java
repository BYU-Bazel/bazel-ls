package server.bazel.interp;

public class InterpRuntimeException extends RuntimeException {
    public InterpRuntimeException() {
        super();
    }

    public InterpRuntimeException(String msg) {
        super(msg);
    }
}
