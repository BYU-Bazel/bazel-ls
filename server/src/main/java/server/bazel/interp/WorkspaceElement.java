package server.bazel.interp;

public final class WorkspaceElement extends Element {
    WorkspaceElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.WORKSPACE;
    }
}
