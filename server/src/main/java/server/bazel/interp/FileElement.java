package server.bazel.interp;

public abstract class FileElement extends Element {
    protected FileElement() {
        super();
    }

    @Override
    public ElementKind kind() {
        return ElementKind.FILE;
    }

    public abstract FileKind fileKind();
}
