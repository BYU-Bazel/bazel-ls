package server.bazel.interp;

public final class WorkspaceElement extends Element {
    public WorkspaceElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.WORKSPACE;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onSync() {
        super.onSync();
    }

    @Override
    public void onFinish() {
        super.onFinish();
    }
}
