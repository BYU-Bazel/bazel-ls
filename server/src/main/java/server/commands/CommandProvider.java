package server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;

import server.bazel.cli.AbstractBazelCommand;
import server.bazel.cli.BazelServerException;
import server.dispatcher.CommandDispatcher;
import server.dispatcher.CommandOutput;
import server.utils.Nullability;
import server.workspace.ExtensionConfig;
import server.workspace.Workspace;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Optional;

/**
 * This class is delegated the task of handling Commands sent by the client
 */
public class CommandProvider {
    private static final Logger logger = LogManager.getLogger(CommandProvider.class);
    private static final CommandDispatcher fallbackDispatcher = CommandDispatcher.create("commandprovider");

    private CommandDispatcher dispatcher;

    public CommandProvider() {
        super();
        dispatcher = null;
    }

    /**
     * Executes a command sent by the client
     *
     * @param params         information about the command sent by the client
     * @param languageClient interface for returning command output and feedback to the client
     * @return an empty object
     */
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params, LanguageClient languageClient) {
        logger.info("Executing command " + params.getCommand() + " with args " + params.getArguments());
        switch (params.getCommand()) {
            case AllCommands.build:
                executeBuildCommand(params.getArguments(), languageClient);
                break;
            case AllCommands.test:
                executeTestCommand(params.getArguments(), languageClient);
                break;
            case AllCommands.none:
                logger.info(params.getCommand() + " was invoked, nothing should happen");
                break;
            case AllCommands.syncServer:
                executeSyncServerCommand(languageClient);
                break;
            default:
                logger.error("Unsupported command: " + params.getCommand());
        }
        return CompletableFuture.completedFuture(new Object());
    }

    /**
     * Requests a server sync to the client. If the client responds with yes, then the
     * sync server command will be executed.
     *
     * @param client an interface with which to return output and feedback to the client.
     */
    public void tryRequestSyncServerCommand(LanguageClient client) {
        ExtensionConfig.SyncMode syncMode = Nullability.nullableOr(ExtensionConfig.SyncMode.commandOnly,
                () -> Workspace.getInstance().getExtensionConfig().getBazel().getSyncMode());
        if (syncMode.equals(ExtensionConfig.SyncMode.showSyncPopup)) {
            ShowMessageRequestParams p = new ShowMessageRequestParams();
            p.setMessage("A file has changed. Would you like to sync the server?");
            p.setType(MessageType.Info);

            List<MessageActionItem> actionItems = new ArrayList<>();
            actionItems.add(new MessageActionItem("Yes"));
            actionItems.add(new MessageActionItem("No"));
            p.setActions(actionItems);

            CompletableFuture<MessageActionItem> result = client.showMessageRequest(p);
            result.thenAccept(s -> {
                logger.info("MessageActionItem completed with value: " + s.getTitle());
                if (s.getTitle().equals("Yes")) {
                    executeSyncServerCommand(client);
                }
            });
        }
    }

    /**
     * Syncs the state of the language server with the contents in memory.
     *
     * @param languageClient an interface with which to return output and feedback to the client.
     */
    public void executeSyncServerCommand(LanguageClient languageClient) {
        try {
            Workspace.getInstance().syncWorkspace();
        } catch (BazelServerException e) {
            MessageParams msg = new MessageParams();
            msg.setType(MessageType.Warning);
            msg.setMessage(String.format("Unable to sync server. Reason: %s", e.getMessage()));
            languageClient.showMessage(msg);
        }
    }

    /**
     * Executes a command to build a _binary BUILD target
     *
     * @param args           contains a String of the path to the BUILD target
     * @param languageClient an interface with which to return output and feedback to the client
     */
    private void executeBuildCommand(List<Object> args, LanguageClient languageClient) {
        String pathString = args.get(0).toString();
        logger.info("path to be built: " + pathString);
        CommandToRun command = new CommandToRun("build", pathString);
        try {
            logger.info("Executing command...");
            final CommandOutput output = runCommand(command);
            languageClient.logMessage(new MessageParams(MessageType.Info, output.getRawErrorOutput()));
            languageClient.showMessage(new MessageParams(MessageType.Info, "Executed target. See language server output console for more detail."));
            logger.info("Command successfully executed");
        } catch (CommandsException e) {
            logger.error("An error occured while trying to execute the command: bazel build " + pathString);
            languageClient.showMessage(new MessageParams(MessageType.Error, "An unexpected error occured."));
        }
    }

    /**
     * Executes a command to run tests in a _test BUILD target
     *
     * @param args           contains a String of the path to the BUILD target
     * @param languageClient an interface with which to return output and feedback to the client
     */
    private void executeTestCommand(List<Object> args, LanguageClient languageClient) {
        String pathString = args.get(0).toString();
        logger.info("path to be built: " + pathString);
        CommandToRun command = new CommandToRun("test", pathString);
        try {
            logger.info("Executing command...");
            final CommandOutput output = runCommand(command);
            languageClient.logMessage(new MessageParams(MessageType.Info, output.getRawErrorOutput()));
            languageClient.showMessage(new MessageParams(MessageType.Info, "Executed target. See language server output console for more detail."));
            logger.info("Command successfully executed");
        } catch (CommandsException e) {
            logger.error("An error occured while trying to execute the command: bazel test " + pathString);
            languageClient.showMessage(new MessageParams(MessageType.Error, "An unexpected error occured."));
        }
    }

    /**
     * Uses the Dispatcher to run a command in the terminal
     *
     * @param command the command to run in the terminal
     * @return the ouput of the command
     * @throws CommandsException if something goes wrong
     */
    private CommandOutput runCommand(AbstractBazelCommand command) throws CommandsException {
        try {
            Optional<CommandOutput> output = getEffectiveDispatcher().dispatch(command);

            if (!output.isPresent()) {
                logger.warn("No output was returned from the bazel command.");
                throw new CommandsException();
            }

            return output.get();
        } catch (InterruptedException e) {
            logger.error(e);
            throw new CommandsException();
        }
    }

    private CommandDispatcher getEffectiveDispatcher() {
        return dispatcher != null ? dispatcher : fallbackDispatcher;
    }
}
