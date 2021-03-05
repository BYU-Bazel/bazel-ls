package server.bazel.interp;

public class RootElement extends Element {
    RootElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.ROOT;
    }
}
