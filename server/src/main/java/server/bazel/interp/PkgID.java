package server.bazel.interp;

public class PkgID extends LabelPart {
    private PkgID(String value) {
        super(value);
    }

    public static PkgID fromString(String value) {
        return new PkgID(value);
    }
}
