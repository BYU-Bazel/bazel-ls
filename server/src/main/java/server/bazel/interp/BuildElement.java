package server.bazel.interp;

public class BuildElement extends FileElement {
    BuildElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.BUILD;
    }

    @Override
    public FileKind fileKind() {
        return FileKind.BUILD;
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

