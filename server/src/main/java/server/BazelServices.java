package server;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

import server.buildifier.Buildifier;
import server.codelens.CodeLensProvider;
import server.codelens.CodeLensResolver;
import server.commands.CommandProvider;
import server.completion.CompletionProvider;
import server.diagnostics.DiagnosticParams;
import server.diagnostics.DiagnosticsProvider;
import server.doclink.DocLinkProvider;
import server.doclink.DocLinkResolver;
import server.formatting.FormattingProvider;
import server.utils.DocumentTracker;
import server.utils.StarlarkWizard;
import server.workspace.ExtensionConfig;
import server.workspace.ProjectFolder;
import server.workspace.Workspace;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class BazelServices implements TextDocumentService, WorkspaceService, LanguageClientAware {
    private static final Logger logger = LogManager.getLogger(BazelServices.class);

    private StarlarkWizard wizard;
    private LanguageClient languageClient;
    private DiagnosticsProvider diagnosticsProvider;
    private CommandProvider commandProvider;
    private DocLinkProvider docLinkProvider;
    private DocLinkResolver docLinkResolver;

    public BazelServices() {
        wizard = new StarlarkWizard();
        languageClient = null;
        diagnosticsProvider = new DiagnosticsProvider();
        commandProvider = new CommandProvider();
        docLinkProvider = new DocLinkProvider();
        docLinkResolver = new DocLinkResolver();
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        logger.info("Did Open");
        DocumentTracker.getInstance().didOpen(params);
        {
            final DiagnosticParams diagnosticParams = new DiagnosticParams();
            diagnosticParams.setWizard(wizard);
            diagnosticParams.setClient(languageClient);
            diagnosticParams.setTracker(DocumentTracker.getInstance());
            diagnosticParams.setUri(URI.create(params.getTextDocument().getUri()));
            diagnosticsProvider.handleDiagnostics(diagnosticParams);
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        logger.info("Did Change");
        DocumentTracker.getInstance().didChange(params);

        // Handle diagnostics.
        {
            final DiagnosticParams diagnosticParams = new DiagnosticParams();
            diagnosticParams.setWizard(wizard);
            diagnosticParams.setClient(languageClient);
            diagnosticParams.setTracker(DocumentTracker.getInstance());
            diagnosticParams.setUri(URI.create(params.getTextDocument().getUri()));
            diagnosticsProvider.handleDiagnostics(diagnosticParams);
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        logger.info("Did Close");
        DocumentTracker.getInstance().didClose(params);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        logger.info("Did Save");

        // Handle sync popups.
        commandProvider.tryRequestSyncServerCommand(languageClient);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
        logger.info("Did Change Configuration");

        // Update extension configuration.
        final Gson gson = new Gson();
        final String json = gson.toJson(params.getSettings());
        final ExtensionConfig config = gson.fromJson(json, ExtensionConfig.class);
        Workspace.getInstance().setExtensionConfig(config);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        logger.info("Did Change Watched Files");
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
        logger.info("Did Change Workspace Folders");

        final Collection<ProjectFolder> foldersToAdd = params.getEvent().getAdded().stream()
                .map(e -> ProjectFolder.fromURI(e.getUri()))
                .collect(Collectors.toList());

        final Collection<ProjectFolder> foldersToRemove = params.getEvent().getRemoved().stream()
                .map(e -> ProjectFolder.fromURI(e.getUri()))
                .collect(Collectors.toList());

        // Update workspace folders.
        Workspace.getInstance().removeWorkspaceFolders(foldersToRemove);
        Workspace.getInstance().addWorkspaceFolders(foldersToAdd);
    }

    @Override
    public void connect(LanguageClient client) {
        languageClient = client;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        return new CompletionProvider().getCompletion(completionParams);
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return CompletableFuture.completedFuture(unresolved);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        logger.info("Formatting request received");
        Buildifier buildifier = new Buildifier();
        // Formatting is done through the buildifier. We must verify that the client has buildifier installed.
        if (buildifier.exists()) {
            FormattingProvider formattingProvider = new FormattingProvider(DocumentTracker.getInstance(), buildifier);
            return formattingProvider.getDocumentFormatting(params);
        } else {
            // Display a popup indicating the client does not have buildifier installed.
            languageClient.showMessage(new MessageParams(MessageType.Info, "Buildifier executable not found.\nPlease install buildifier to enable file formatting."));
            return CompletableFuture.completedFuture(new ArrayList<TextEdit>());
        }
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        logger.info("CodeLens request received");
        CodeLensProvider codeLensProvider = new CodeLensProvider(DocumentTracker.getInstance());
        return codeLensProvider.getCodeLens(params);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        logger.info("CodeLens resolve request received");
        CodeLensResolver codeLensResolver = new CodeLensResolver();
        return codeLensResolver.resolveCodeLens(unresolved);
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
        logger.info(String.format("Executing command: %s", params));
        return commandProvider.executeCommand(params, languageClient);
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
        logger.info(String.format("Handling document link: %s", params));
        docLinkProvider.setTracker(DocumentTracker.getInstance());
        docLinkProvider.setWizard(wizard);
        return docLinkProvider.handleDocLink(params);
    }

    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
        logger.info(String.format("Resolving document link: %s", params));
        return docLinkResolver.resolveDocLink(params);
    }

    public void sendMessageToClient(MessageType type, String message) {
        languageClient.showMessage(new MessageParams(type, message));
    }
}