package server.bazel.interp;

public class WorkspaceFileNode extends FileNode {
    WorkspaceFileNode(UniqueID id) {
        super(id);
    }

    @Override
    public SourceGraphNodeKind nodeKind() {
        return SourceGraphNodeKind.FILE;
    }

    @Override
    public FileKind fileKind() {
        return FileKind.WORKSPACE;
    }
}
