package server.bazel.interp;

public class TargetElement extends Element {
    TargetElement() {
        super();
    }

    @Override
    public ElementKind elementKind() {
        return ElementKind.TARGET;
    }
}
