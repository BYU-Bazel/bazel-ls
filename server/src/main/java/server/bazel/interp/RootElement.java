package server.bazel.interp;

public class RootElement extends Element {
    RootElement() {

    }

    @Override
    public ElementKind kind() {
        return ElementKind.ROOT;
    }
}
