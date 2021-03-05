package server.bazel.interp;

public class GraphNodeParams<T extends Element> {
    private T element;
    private UniqueID declaringNodeId;

    public T getElement() {
        return element;
    }

    public void setElement(T element) {
        this.element = element;
    }

    public UniqueID getDeclaringNodeId() {
        return declaringNodeId;
    }

    public void setDeclaringNodeId(UniqueID declaringNodeId) {
        this.declaringNodeId = declaringNodeId;
    }
}
