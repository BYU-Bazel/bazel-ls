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
}
