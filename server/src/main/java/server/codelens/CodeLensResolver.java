package server.codelens;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;

public class CodeLensResolver {
    
    private static final Logger logger = LogManager.getLogger(CodeLensResolver.class);

    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        logger.info("CodeLens Resolver invoked");
        return null;
    }
}
