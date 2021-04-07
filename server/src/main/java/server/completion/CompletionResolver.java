package server.completion;

import org.eclipse.lsp4j.CompletionItem;

import java.util.concurrent.CompletableFuture;

public class CompletionResolver {
    public CompletionResolver() {
        super();
    }

    public CompletableFuture<CompletionItem> resolve(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }
}
