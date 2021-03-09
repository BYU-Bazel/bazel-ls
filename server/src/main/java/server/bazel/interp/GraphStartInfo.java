package server.bazel.interp;

public class GraphStartInfo {
    private UniqueID startNodeID;

    public GraphStartInfo() {
        super();
    }

    public UniqueID startNodeID() {
        return startNodeID;
    }

    public boolean hasStartNodeID() {
        return startNodeID() != null;
    }

    public void setStartNodeID(UniqueID startNodeID) {
        this.startNodeID = startNodeID;
    }
}
