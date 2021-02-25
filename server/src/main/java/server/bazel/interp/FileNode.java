package server.bazel.interp;

public abstract class FileNode extends SourceGraphNode {
    FileNode(UniqueID id) {
        super(id);
    }

    @Override
    public SourceGraphNodeKind nodeKind() {
        return SourceGraphNodeKind.FILE;
    }

    public abstract FileKind fileKind();
}
