package server.bazel.interp;

import java.nio.file.Path;

public class LabelResolveOutput {
    private Path path;

    public LabelResolveOutput() {
        super();
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }
}
