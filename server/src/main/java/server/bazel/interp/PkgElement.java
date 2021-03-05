package server.bazel.interp;

public class PkgElement extends Element {
    @Override
    public ElementKind kind() {
        return ElementKind.PKG;
    }
}
