package server.bazel.interp;

public class PkgElement extends Element {
    @Override
    public ElementKind elementKind() {
        return ElementKind.PKG;
    }
}
