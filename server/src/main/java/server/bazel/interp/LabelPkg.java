package server.bazel.interp;

public class LabelPkg extends LabelPart {
    private LabelPkg(String value) {
        super(value);
    }

    public static LabelPkg fromString(String value) {
        return new LabelPkg(value);
    }
}
