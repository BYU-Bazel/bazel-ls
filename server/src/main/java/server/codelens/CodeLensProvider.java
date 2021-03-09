package server.codelens;

import java.util.concurrent.CompletableFuture;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import server.utils.DocumentTracker;

public class CodeLensProvider {

    private static final Logger logger = LogManager.getLogger(CodeLensProvider.class);

    private DocumentTracker documentTracker;

    public CodeLensProvider(DocumentTracker documentTracker) {
        this.documentTracker = documentTracker;
    }

    public CompletableFuture<List<? extends CodeLens>> getCodeLens(CodeLensParams params) {
        logger.info("CodeLens Provider invoked");

        String contents = "";
        try {
            contents = documentTracker.getContents(new URI(params.getTextDocument().getUri()));
        } catch (Exception e) {
            logger.error(e);
        }

        CodeLens dummy = new CodeLens();
        Range range = new Range(new Position(8, 0), new Position(8, 9));
        Command command = new Command();
        command.setTitle("Do Nothing");
        command.setCommand(CommandConstants.none);
        dummy.setRange(range);
        dummy.setCommand(command);


        return CompletableFuture.completedFuture(Arrays.asList(dummy));
    }

}
