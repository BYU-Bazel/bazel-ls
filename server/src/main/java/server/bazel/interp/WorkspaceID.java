package server.bazel.interp;

public class WorkspaceID extends LabelPart {
    private WorkspaceID(String value) {
        super(value);
    }

    public static WorkspaceID fromString(String value) {
        return new WorkspaceID(value);
    }
}
