package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInfo {
    private final Path path;
    private final FileKind kind;
    private final String content;

    private FileInfo(Path path, FileKind kind, String content) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(kind);
        Preconditions.checkNotNull(content);
        this.path = path;
        this.kind = kind;
        this.content = content;
    }

    public static FileInfo fromPath(Path path) throws IOException {
        Preconditions.checkNotNull(path);
        final String content = Files.readString(path);
        return FileInfo.fromPathWithContent(path, content);
    }

    public static FileInfo fromPathWithContent(Path path, String content) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);
        final FileKind kind = FileKind.inferFrom(path);
        return new FileInfo(path, kind, content);
    }

    public Path path() {
        return path;
    }

    public FileKind kind() {
        return kind;
    }

    public String content() {
        return content;
    }

    public UniqueID uniqueID() {
        return UniqueID.fromPath(path());
    }
}
