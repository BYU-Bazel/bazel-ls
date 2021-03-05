package server.bazel.interp;

public class TargetElement extends Element {
    TargetElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.TARGET;
    }
}
