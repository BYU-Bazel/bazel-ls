package server.bazel.interp;

// TODO: This should be the :xyz part OR the source file /safas/asdfasd/asdf/<<SOURCE_FILE.cc>> <-- This part
public class TargetID extends LabelPart {
    private TargetID(String value) {
        super(value);
    }

    public static TargetID fromString(String value) {
        return new TargetID(value);
    }
}
