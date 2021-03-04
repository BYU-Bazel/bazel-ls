package server.codelens;

import java.util.concurrent.CompletableFuture;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;

import server.utils.DocumentTracker;

public class CodeLensProvider {
    
    private static final Logger logger = LogManager.getLogger(CodeLensProvider.class);

    private DocumentTracker documentTracker;
    
    public CodeLensProvider(DocumentTracker documentTracker) {
        this.documentTracker = documentTracker;
    }

    public CompletableFuture<List<? extends CodeLens>> getCodeLens(CodeLensParams params) {
        logger.info("CodeLens Provider invoked");
        return null;
    }
}