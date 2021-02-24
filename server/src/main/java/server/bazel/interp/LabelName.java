package server.bazel.interp;

public class LabelName extends LabelPart {
    private LabelName(String value) {
        super(value);
    }

    public static LabelName fromString(String value) {
        return new LabelName(value);
    }
}
