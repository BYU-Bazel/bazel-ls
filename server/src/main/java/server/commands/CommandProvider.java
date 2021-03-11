package server.commands;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.ExecuteCommandParams;

import server.bazel.cli.AbstractBazelCommand;
import server.dispatcher.CommandDispatcher;
import server.dispatcher.CommandOutput;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Optional;

public class CommandProvider {
    private static final Logger logger = LogManager.getLogger(CommandProvider.class);
    private static final CommandDispatcher fallbackDispatcher = CommandDispatcher.create("commandprovider");

    private CommandDispatcher dispatcher;

    public CommandProvider() {
        super();
        dispatcher = null;
    }

    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        logger.info("Executing command " + params.getCommand() + " with args " + params.getArguments());
        String output = "";
        switch(params.getCommand()) {
            case AllCommands.build:
                output = executeBuildCommand(params.getArguments());
                break;
            case AllCommands.test:
                output = executeTestCommand(params.getArguments());
                break;
            case AllCommands.none:
                logger.info(params.getCommand() + " was invoked, nothing should happen");
                break;
            default:
                logger.error("Unsupported command: " + params.getCommand());
        }
        return CompletableFuture.completedFuture(output);
    }

    private String executeBuildCommand(List<Object> args) {
        String pathString = args.get(0).toString();
        logger.info("path to be built: " + pathString);
        String returnOutput = "";
        CommandToRun command = new CommandToRun("build", pathString);
        try {
            logger.info("Executing command...");
            final CommandOutput output = runCommand(command);
            if (output.didSucceed()) {
                logger.info(String.format("Successfully ran command, output returned: %s", output.getRawStandardOutput()));
                returnOutput = output.getRawStandardOutput();
            } else {
                logger.info(String.format("Command failed, output returned: %s", output.getRawErrorOutput()));
                returnOutput = output.getRawErrorOutput();
            }
        } catch(CommandsException e) {
            logger.error("An error occured while trying to execute the command: bazel build " + pathString);
        }
        return returnOutput;
    }

    private String executeTestCommand(List<Object> args) {
        String pathString = args.get(0).toString();
        logger.info("path to be built: " + pathString);
        String returnOutput = "";
        CommandToRun command - new CommandToRun("test", pathString);
        try {
            logger.info("Executing command...");
            final CommandOutput output = runCommand(command);
            if (output.didSucceed()) {
                logger.info(String.format("Successfully ran test, output returned: %s", output.getRawStandardOutput()));
                returnOutput = output.getRawStandardOutput();
            } else {
                logger.info(String.format("Test failed, output returned: %s", output.getRawErrorOutput()));
                returnOutput = output.getRawErrorOutput();
            }
        } catch(CommandsException e) {
            logger.error("An error occured while trying to execute the command: bazel test " + pathString);
        }
        return returnOutput;
    }

    private CommandOutput runCommand(AbstractBazelCommand command) throws CommandsException {
        try {
            Optional<CommandOutput> output = getEffectiveDispatcher().dispatch(command);

            if(!output.isPresent()) {
                logger.warn("No output was returned from the bazel command.");
                throw new CommandsException();
            }

            return output.get();
        } catch(InterruptedException e) {
            logger.error(e);
            throw new CommandsException();
        }
    }

    private CommandDispatcher getEffectiveDispatcher() {
        return dispatcher != null ? dispatcher : fallbackDispatcher;
    }
}
