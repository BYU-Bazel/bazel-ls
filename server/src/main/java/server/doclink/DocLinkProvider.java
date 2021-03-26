package server.doclink;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import server.utils.DocumentTracker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DocLinkProvider {
    private static final Logger logger = LogManager.getLogger(DocLinkProvider.class);

    private DocumentTracker tracker = null;

    public DocumentTracker getTracker() {
        return tracker;
    }

    public void setTracker(DocumentTracker tracker) {
        this.tracker = tracker;
    }

    public CompletableFuture<List<DocumentLink>> handleDocLink(DocumentLinkParams params) {
        Preconditions.checkNotNull(tracker);

        final List<DocumentLink> result = new ArrayList<>();
        final Path filePath = Paths.get(params.getTextDocument().getUri());
        final DocumentTracker tracker = getTracker();
        final String content = tracker.getContents(filePath.toUri());

        return CompletableFuture.completedFuture(result);
    }
}
