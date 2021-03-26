package server.doclink;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.starlark.java.syntax.Expression;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.StringLiteral;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.DocumentLink;
import org.eclipse.lsp4j.DocumentLinkParams;
import server.bazel.interp.*;
import server.utils.DocumentTracker;
import server.utils.FileRepository;
import server.utils.Logging;
import server.utils.StarlarkWizard;
import server.workspace.Workspace;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DocLinkProvider {
    private static final Logger logger = LogManager.getLogger(DocLinkProvider.class);

    private DocumentTracker tracker = null;
    private StarlarkWizard wizard = null;
    private Path currentDocPath = null;

    public DocumentTracker getTracker() {
        return tracker;
    }

    public void setTracker(DocumentTracker tracker) {
        this.tracker = tracker;
    }

    public StarlarkWizard getWizard() {
        return wizard;
    }

    public void setWizard(StarlarkWizard wizard) {
        this.wizard = wizard;
    }

    public CompletableFuture<List<DocumentLink>> handleDocLink(DocumentLinkParams params) {
        Preconditions.checkNotNull(getTracker());
        Preconditions.checkNotNull(getWizard());


        final List<DocumentLink> result = new ArrayList<>();
        final URI fileURI = URI.create(params.getTextDocument().getUri());
        final Path filePath = Paths.get(fileURI).toAbsolutePath();
        final DocumentTracker tracker = getTracker();
        final String content = tracker.getContents(filePath.toUri());

        // Keep references for context when running this provider.
        currentDocPath = filePath;

        // Parse the starlark file.
        final StarlarkFile file;
        try {
            final ParserInput input = ParserInput.fromString(content, params.getTextDocument().getUri());
            file = StarlarkFile.parse(input);
        } catch (Error | RuntimeException e) {
            logger.error("Parsing failed for an unknown reason!");
            logger.error(Logging.stackTraceToString(e));
            return CompletableFuture.completedFuture(result);
        }

        // Locate all linkable content and create document links.
        final ImmutableList<StarlarkWizard.TargetMeta> targets = getWizard().locateTargets(file);
        for (final StarlarkWizard.TargetMeta target : targets) {
            result.addAll(convertLabelExprs2DocLinks(target.srcs()));
            result.addAll(convertLabelExprs2DocLinks(target.deps()));
        }

        // Lose references for context after running this provider.
        currentDocPath = null;

        return CompletableFuture.completedFuture(result);
    }

    private Collection<DocumentLink> convertLabelExprs2DocLinks(Iterable<Expression> expressions) {
        final List<DocumentLink> result = new ArrayList<>();

        for (final Expression expr : expressions) {
            final DocumentLink link = labelExpr2DocLink(expr);
            if (link == null) {
                continue;
            }

            result.add(link);
        }

        return result;
    }

    private DocumentLink labelExpr2DocLink(Expression expr) {
        final DocumentLink link = new DocumentLink();

        // Infer the URI and tooltip based on the parsed label.
        try {
            if (expr.kind() != Expression.Kind.STRING_LITERAL) {
                return null;
            }

            // Parse a label from the string value.
            final StringLiteral literal = (StringLiteral) expr;
            final Label label = Label.parse(literal.getValue());

            // We're not supporting external workspaces right now.
            if (label.hasWorkspace()) {
                return null;
            }

            final LabelResolveInput input = new LabelResolveInput();
            input.setFileRepository(FileRepository.getDefault());
            input.setLocalDeclaringFilePath(currentDocPath);
            input.setLocalWorkspacePath(Workspace.getInstance().getRootFolder().getPath());

            final LabelResolveOutput output = label.resolve(input);
            link.setTarget(output.getPath().toUri().toString());
        } catch (LabelSyntaxException e) {
            return null;
        } catch (LabelNotFoundException e) {
            logger.info("Label " + expr.toString() + " could not be converted to a link " +
                    "the target doesn't exist.");
            return null;
        }

        // Keep a static tooltip for now.
        link.setTooltip("Links to a local target.");

        // The range should always be the length of the expression.
        link.setRange(getWizard().rangeFromExpression(expr));

        return link;
    }
}
