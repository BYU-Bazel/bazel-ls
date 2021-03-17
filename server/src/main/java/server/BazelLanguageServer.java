package server;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.*;
import server.commands.AllCommands;
import server.workspace.ProjectFolder;
import server.workspace.Workspace;
import server.bazel.cli.BazelServerException;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class BazelLanguageServer implements LanguageServer, LanguageClientAware {
    /**
     * Toggle this flag to enable/disable logging.
     */
    private static final boolean DEBUG = true;

    private static final int EXIT_SUCCESS = 0;
    private final Logger logger = LogManager.getLogger(BazelLanguageServer.class);

    public static void main(String[] args) {
//        System.out.println("HEREEEEE");
        if (DEBUG) {
//            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
//
//            AppenderComponentBuilder rollingFile
//                    = builder.newAppender("rolling", "RollingFile");
//            rollingFile.addAttribute("fileName", "rolling.log");
//            rollingFile.addAttribute("filePattern", "rolling-%d{MM-dd-yy}.log.gz");
//            builder.add(rollingFile);
//
//            ComponentBuilder<?> triggeringPolicies = builder.newComponent("Policies")
//                    .addComponent(builder.newComponent("OnStartUpTriggeringPolicy"));
//            rollingFile.addComponent(triggeringPolicies);
//
//            RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.ALL);
//            rootLogger.add(builder.newAppenderRef("rolling"));
//            builder.add(rootLogger);
//
////            System.out.println("PRE INIT");
//            Configurator.initialize(builder.build());
//            LogManager.getLogger(BazelLanguageServer.class).info("FINISHED!");

//            System.out.println("POST INIT");
        }
//        System.out.println("AFTER DEBUG");

        final BazelLanguageServer server = new BazelLanguageServer();
        final Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class,
                System.in, System.out);

        server.connect(launcher.getRemoteProxy());
        launcher.startListening();
    }

    private BazelServices bazelServices;

    public BazelLanguageServer() {
        bazelServices = new BazelServices();
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        logger.info(String.format("Starting up bazel language server with params:\n\"%s\"", params));

        initializeWorkspaceRoot(params);
        try {
            Workspace.getInstance().initializeWorkspace();
            logger.info("workspace initialized");

        } catch (BazelServerException e) {
            logger.info("workspace error");
            String message = "Bazel Extension Failed to parse due to BUILD Parsing errors:\n";
            String fix = "Please fix the Bazel Syntax error then restart the Extension\n";
            bazelServices.sendMessageToClient(MessageType.Error, message + fix + e.getMessage());
        }

        return CompletableFuture.completedFuture(specifyServerCapabilities());
    }

    private InitializeResult specifyServerCapabilities() {
        ServerCapabilities serverCapabilities = new ServerCapabilities();

        serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        serverCapabilities.setCompletionProvider(new CompletionOptions(true, Arrays.asList(":", "/", "\"")));
        serverCapabilities.setDocumentFormattingProvider(true);
        serverCapabilities.setCodeLensProvider(new CodeLensOptions(true));
        serverCapabilities.setExecuteCommandProvider(new ExecuteCommandOptions(AllCommands.allCommands()));

        logger.info(String.format("Declared server capabilities: \"%s\"", serverCapabilities));

        return new InitializeResult(serverCapabilities);
    }

    private void initializeWorkspaceRoot(InitializeParams params) {
        final ProjectFolder folder = ProjectFolder.fromURI(params.getRootUri());
        Workspace.getInstance().setRootFolder(folder);

        logger.info(String.format("Declared root folder: \"%s\"", Workspace.getInstance().getRootFolder()));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(new Object());
    }

    @Override
    public void exit() {
        System.exit(EXIT_SUCCESS);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return bazelServices;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return bazelServices;
    }

    @Override
    public void connect(LanguageClient client) {
        bazelServices.connect(client);
    }
}
