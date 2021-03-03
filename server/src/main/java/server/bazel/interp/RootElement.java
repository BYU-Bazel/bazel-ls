package server.bazel.interp;

public class RootElement extends Element {
    RootElement() {
        super();
    }

    @Override
    public ElementKind elementKind() {
        return ElementKind.ROOT;
    }
}
