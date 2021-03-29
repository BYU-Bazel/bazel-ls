package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private static final int EXIT_SUCCESS = 0;
    private static final Logger logger = LogManager.getLogger(BazelLanguageServer.class);

    public static void main(String[] args) {
        final BazelLanguageServer server = new BazelLanguageServer();
        final Launcher<LanguageClient> launcher = Launcher.createLauncher(server, LanguageClient.class,
                System.in, System.out);

        logger.info("Launching server...");
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
        return CompletableFuture.completedFuture(specifyServerCapabilities());
    }

    private InitializeResult specifyServerCapabilities() {
        ServerCapabilities serverCapabilities = new ServerCapabilities();

        serverCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
        serverCapabilities.setCompletionProvider(new CompletionOptions(true, Arrays.asList(":", "/", "\"")));
        serverCapabilities.setDocumentFormattingProvider(true);
        serverCapabilities.setCodeLensProvider(new CodeLensOptions(true));
        serverCapabilities.setExecuteCommandProvider(new ExecuteCommandOptions(AllCommands.allCommands()));
        serverCapabilities.setDocumentLinkProvider(new DocumentLinkOptions(true));

        logger.info(String.format("Declared server capabilities: \"%s\"", serverCapabilities));

        return new InitializeResult(serverCapabilities);
    }

    private void initializeWorkspaceRoot(InitializeParams params) {
        final ProjectFolder folder = ProjectFolder.fromURI(params.getRootUri());
        Workspace.getInstance().setRootFolder(folder);

        try {
            Workspace.getInstance().syncWorkspace();
            logger.info("workspace syned");
        } catch (BazelServerException e) {
            logger.info("workspace error");
            String message = "Bazel Extension Failed to parse due to BUILD Parsing errors:\n";
            String fix = "Please fix the Bazel Syntax error then restart the Extension\n";
            bazelServices.sendMessageToClient(MessageType.Error, message + fix + e.getMessage());
        }

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
