package server.bazel.interp;

import com.google.common.base.Preconditions;

import java.util.Objects;

public abstract class LabelPart {
    private final String value;

    protected LabelPart(String value) {
        Preconditions.checkNotNull(value);
        Preconditions.checkArgument(!value.isEmpty(), "A label part must not be empty.");
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LabelPart)) return false;
        LabelPart that = (LabelPart) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
