package server.bazel.interp;

// TODO: Maybe needs a different name, more consistent alphatbetically with workspace files.
public class LabelWorkspace extends LabelPart {
    private LabelWorkspace(String value) {
        super(value);
    }

    public static LabelWorkspace fromString(String value) {
        return new LabelWorkspace(value);
    }
}
