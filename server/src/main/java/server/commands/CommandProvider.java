package server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.ExecuteCommandParams;

import java.util.concurrent.CompletableFuture;

public class CommandProvider {
    private static final Logger logger = LogManager.getLogger(CommandProvider.class);

    public CommandProvider() {
        super();
    }

    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        logger.info("I AM EXECUTING A COMMAND!!! " + params.toString());
        return null;
    }
}
