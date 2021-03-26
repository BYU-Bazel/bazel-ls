package server.bazel.interp;

import server.utils.FileRepository;

import java.nio.file.Path;

public final class LabelResolveInput {
    private Path localWorkspacePath;
    private Path localDeclaringFilePath;
    private FileRepository fileRepository;

    public LabelResolveInput() {
        super();
        fileRepository = FileRepository.getDefault();
    }

    public FileRepository getFileRepository() {
        return fileRepository;
    }

    public void setFileRepository(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public Path getLocalWorkspacePath() {
        return localWorkspacePath;
    }

    public void setLocalWorkspacePath(Path localWorkspacePath) {
        this.localWorkspacePath = localWorkspacePath;
    }

    public Path getLocalDeclaringFilePath() {
        return localDeclaringFilePath;
    }

    public void setLocalDeclaringFilePath(Path localDeclaringFilePath) {
        this.localDeclaringFilePath = localDeclaringFilePath;
    }
}
