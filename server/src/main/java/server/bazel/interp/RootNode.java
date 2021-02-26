package server.bazel.interp;

public class RootNode extends SourceGraphNode {
    public static final UniqueID DEFAULT_ID = UniqueID.raw("root_node_id", "root_node_id");

    RootNode() {
        super(DEFAULT_ID);
    }

    @Override
    public SourceGraphNodeKind nodeKind() {
        return SourceGraphNodeKind.ROOT;
    }
}
