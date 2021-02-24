package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class FileRange {
    private final FilePosition start;
    private final FilePosition end;

    private FileRange(FilePosition start, FilePosition end) {
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);
        this.start = start;
        this.end = end;
    }

    public static FileRange fromStartEnd(FilePosition start, FilePosition end) {
        return new FileRange(start, end);
    }

    public FilePosition start() {
        return start;
    }

    public FilePosition end() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileRange)) return false;
        FileRange fileRange = (FileRange) o;
        return Objects.equals(start, fileRange.start) &&
                Objects.equals(end, fileRange.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "FileRange{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
