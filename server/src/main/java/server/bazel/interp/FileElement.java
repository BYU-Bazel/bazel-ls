package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.nio.file.Path;

public class FileElement extends OldGraphNode {
    private final Path path;

    FileElement(OldGraphComponent.Args args, Path path) {
        super(args);
        Preconditions.checkNotNull(path);
        this.path = path;
    }

    public Path path() {
        return path;
    }

    @Override
    public ElementKind nodeKind() {
        return ElementKind.FILE;
    }

    public abstract FileKind fileKind();
}
