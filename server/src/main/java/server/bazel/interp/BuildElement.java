package server.bazel.interp;

public class BuildElement extends FileElement {
    BuildElement() {
        super();
    }

    @Override
    public ElementKind elementKind() {
        return ElementKind.BUILD;
    }

    @Override
    public FileKind fileKind() {
        return FileKind.BUILD;
    }
}
