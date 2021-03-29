package server.diagnostics;

import org.eclipse.lsp4j.services.LanguageClient;
import server.utils.DocumentTracker;
import server.utils.StarlarkWizard;

import java.net.URI;

public class DiagnosticParams {
    private LanguageClient client;
    private DocumentTracker tracker;
    private StarlarkWizard wizard;
    private URI uri;

    public DiagnosticParams() {
        super();
    }

    public LanguageClient getClient() {
        return client;
    }

    public void setClient(LanguageClient client) {
        this.client = client;
    }

    public DocumentTracker getTracker() {
        return tracker;
    }

    public void setTracker(DocumentTracker tracker) {
        this.tracker = tracker;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public StarlarkWizard getWizard() {
        return wizard;
    }

    public void setWizard(StarlarkWizard wizard) {
        this.wizard = wizard;
    }

    @Override
    public String toString() {
        return "DiagnosticParams{" +
                "client=" + client +
                ", tracker=" + tracker +
                ", wizard=" + wizard +
                ", uri=" + uri +
                '}';
    }
}
