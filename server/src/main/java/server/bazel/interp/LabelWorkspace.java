package server.bazel.interp;

public class LabelWorkspace extends LabelPart {
    private LabelWorkspace(String value) {
        super(value);
    }

    public static LabelWorkspace fromString(String value) {
        return new LabelWorkspace(value);
    }
}
