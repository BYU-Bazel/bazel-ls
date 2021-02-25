package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Exceptions;

import java.nio.file.Path;

public enum FileKind {
    BAZEL,
    BUILD,
    WORKSPACE;

    static FileKind inferFrom(String filename) {
        Preconditions.checkNotNull(filename);
        Preconditions.checkArgument(!filename.contains("/"));

        FileKind kind;
        if (filename.equals("WORKSPACE") || filename.equals("WORKSPACE.bazel")) {
            kind = FileKind.WORKSPACE;
        } else if (filename.equals("BUILD") || filename.equals("BUILD.bazel")) {
            kind = FileKind.BUILD;
        } else if (filename.endsWith(".bzl")) {
            kind = FileKind.BAZEL;
        } else {
            throw new Exceptions.Unimplemented(String.format("%s for file \"%s\" not implemented.",
                    FileKind.class.toString(), filename));
        }

        return kind;
    }

    static FileKind inferFrom(Path path) {
        Preconditions.checkNotNull(path);
        final String filename = path.getFileName().toString();
        return inferFrom(filename);
    }
}
