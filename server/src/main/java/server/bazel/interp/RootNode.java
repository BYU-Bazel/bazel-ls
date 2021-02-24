package server.bazel.interp;

public class RootNode extends SourceGraphNode {
    public static final UniqueID ID = UniqueID.custom("root_node_id", "root_node_id");

    RootNode() {
        super(ID);
    }
}
