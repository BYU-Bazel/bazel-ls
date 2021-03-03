package server.bazel.interp;

public abstract class FileElement extends Element {
    protected FileElement() {
        super();
    }

    @Override
    public ElementKind elementKind() {
        return ElementKind.FILE;
    }

    public abstract FileKind fileKind();
}
