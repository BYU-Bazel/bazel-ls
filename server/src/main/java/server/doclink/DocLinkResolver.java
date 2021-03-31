package server.doclink;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.DocumentLink;

import java.util.concurrent.CompletableFuture;

public class DocLinkResolver {
    private static final Logger logger = LogManager.getLogger(DocLinkResolver.class);

    public CompletableFuture<DocumentLink> resolveDocLink(DocumentLink link) {
        logger.info("Resolving link " + link);
        return CompletableFuture.completedFuture(link);
    }
}
