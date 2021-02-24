package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Exceptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileInformation {
    private final Path path;
    private final FileKind kind;
    private final String content;

    private FileInformation(Path path, FileKind kind, String content) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(kind);
        Preconditions.checkNotNull(content);

        this.path = path;
        this.kind = kind;
        this.content = content;
    }

    public static FileInformation fromPath(Path path) throws IOException {
        Preconditions.checkNotNull(path);
        final String content = Files.readString(path);
        return FileInformation.fromPathWithContent(path, content);
    }

    public static FileInformation fromPathWithContent(Path path, String content) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);

        final FileKind kind = InterpUtility.inferFileKind(path);
        return new FileInformation(path, kind, content);
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
}
