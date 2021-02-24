package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class FilePosition {
    private final int row;
    private final int col;

    private FilePosition(int row, int col) {
        Preconditions.checkArgument(row >= 0);
        Preconditions.checkArgument(col >= 0);
        this.row = row;
        this.col = col;
    }

    public static FilePosition fromRowCol(int row, int col) {
        return new FilePosition(row, col);
    }

    public int row() {
        return row;
    }

    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FilePosition)) return false;
        FilePosition that = (FilePosition) o;
        return row == that.row &&
                col == that.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "FilePosition{" +
                "row=" + row +
                ", col=" + col +
                '}';
    }
}
