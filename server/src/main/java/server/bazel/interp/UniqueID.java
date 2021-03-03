package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class UniqueID {
    final String value;
    final String friendly;

    private UniqueID(String value, String friendly) {
        Preconditions.checkNotNull(value);
        Preconditions.checkNotNull(friendly);
        this.value = value;
        this.friendly = friendly;
    }

    public static UniqueID raw(String value, String friendly) {
        return new UniqueID(value, friendly);
    }

    public static UniqueID random() {
        final String uid = UUID.randomUUID().toString();
        final String friendly = "Completely random ID";
        return UniqueID.raw(uid, friendly);
    }

    public static UniqueID fromPath(Path path) {
        final String abs = path.toAbsolutePath().toString();
        final String uid = Utility.hash(abs);
        return UniqueID.raw(uid, abs);
    }

    public static UniqueID fromStartEndNodes(OldGraphNode start, OldGraphNode end) {
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);

        final String uid = Utility.hash(start.id().value + end.id().value);
        final String friendly = "ID representing a path between two nodes";
        return UniqueID.raw(uid, friendly);
    }

    public String value() {
        return value;
    }

    public String friendly() {
        return friendly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UniqueID)) return false;
        UniqueID uniqueID = (UniqueID) o;
        return Objects.equals(value, uniqueID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "UniqueID{" +
                "value='" + value + '\'' +
                ", friendly='" + friendly + '\'' +
                '}';
    }
}
