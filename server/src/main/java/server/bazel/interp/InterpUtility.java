package server.bazel.interp;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import server.utils.Exceptions;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

class InterpUtility {
    private InterpUtility() {
    }

    public static FileKind inferFileKind(String filename) {
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

    public static FileKind inferFileKind(Path path) {
        Preconditions.checkNotNull(path);
        final String filename = path.getFileName().toString();
        return inferFileKind(filename);
    }

    public static String hash(String value) {
        Preconditions.checkNotNull(value);
        return Hashing.sha256().hashString(value, StandardCharsets.UTF_8).toString();
    }
}
