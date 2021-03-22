package server.diagnostics;

import com.google.common.base.Preconditions;
import net.starlark.java.syntax.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;
import server.bazel.bazelWorkspaceAPI.WorkspaceAPI;
import server.bazel.interp.CompatabilityUtility;
import server.bazel.interp.Label;
import server.bazel.interp.LabelSyntaxException;
import server.bazel.tree.BuildTarget;
import server.bazel.tree.WorkspaceTree;
import server.utils.DocumentTracker;
import server.utils.Logging;
import server.workspace.Workspace;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// TODO: This should be the analysis stuff.

/**
 * Provides diagnostics for a given bazel file. Note, this class is purely a prototype.
 * This class will be overruled when we figure out how to do generic Bazel interpretting.
 */
public class DiagnosticsProvider {
    private static final Logger logger = LogManager.getLogger(DiagnosticsProvider.class);

    public DiagnosticsProvider() {
        super();
    }

    private List<Expression> locatePossibleLabelExpressions(StarlarkFile file) {
        final List<Expression> result = new ArrayList<>();

        for (final Statement stmt : file.getStatements()) {
            if (stmt.kind() == Statement.Kind.EXPRESSION) {
                final Expression expr = ((ExpressionStatement) stmt).getExpression();
                if (expr.kind() == Expression.Kind.CALL) {
                    // Check all arguments of call functions
                    final CallExpression call = (CallExpression) expr;
                    for (final Argument arg : call.getArguments()) {
                        if (arg.getName() == null) {
                            continue;
                        }

                        // If this is the deps attribute or sources attribute.
                        if (arg.getName().equals("deps") || arg.getName().equals("srcs")) {
                            final Expression argExpr = arg.getValue();

                            // If this is a list (should usually be a list)
                            if (argExpr.kind() == Expression.Kind.LIST_EXPR) {
                                final ListExpression listExpr = (ListExpression) argExpr;
                                result.addAll(listExpr.getElements());
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    // TODO: Cleanup
    public void handleDiagnostics(DiagnosticParams params) {
        logger.info(String.format("Handling diagnostics for params:\n%s", params));

        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(params.getClient());
        Preconditions.checkNotNull(params.getTracker());
        Preconditions.checkNotNull(params.getUri());

        final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
        final WorkspaceAPI api = new WorkspaceAPI(tree);

        final URI textDocURI = params.getUri();
        final Path textDocPath = Paths.get(textDocURI);
        final String textDocContent = DocumentTracker.getInstance().getContents(textDocURI);

        // Parse the starlark file.
        logger.info("Attempting to parse starlark file for syntax highlighting.");
        final StarlarkFile file;
        try {
            final ParserInput input = ParserInput.fromString(textDocContent, textDocURI.toString());
            file = StarlarkFile.parse(input);
        } catch (Error | RuntimeException e) {
            logger.error("Parsing failed for an unknown reason!");
            logger.error(Logging.stackTraceToString(e));
            return;
        }

        // Keep track of all diagnostics to handle/display to the user.
        final List<Diagnostic> diagnostics = new ArrayList<>();

        // Highlight all syntax errors.
        for (final SyntaxError err : file.errors()) {
            final Diagnostic diagnostic = new Diagnostic();
            diagnostic.setSeverity(DiagnosticSeverity.Error);
            diagnostic.setCode(DiagnosticCodes.SYNTAX_ERROR);
            diagnostic.setMessage(err.message());

            // There's no apparent way to know where the error ends. Highlight to the end of the line.
            final Range range = new Range();
            range.setStart(new Position(err.location().line() - 1, err.location().column()));
            range.setEnd(new Position(err.location().line() - 1, 9999));

            diagnostic.setRange(range);
            diagnostics.add(diagnostic);
        }

        // Interpret all labels.
        final List<Expression> labelExpressions = locatePossibleLabelExpressions(file);
        for (Expression expr : labelExpressions) {
            final int line = expr.getStartLocation().line() - 1;
            final int colstart = expr.getStartLocation().column();
            final int colend = expr.getEndLocation().column();
            final Range range = new Range(new Position(line, colstart), new Position(line, colend));

            // Labels must be strings.
            if (expr.kind() != Expression.Kind.STRING_LITERAL) {
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Error);
                diag.setMessage("A label must be a string.");
                diag.setCode(DiagnosticCodes.INVALID_TARGET);
                diag.setRange(range);
                diagnostics.add(diag);
                continue;
            }

            final StringLiteral labelStr = (StringLiteral) expr;
            final Label label;

            // Validate the label syntax.
            try {
                label = Label.parse(labelStr.getValue());
            } catch (LabelSyntaxException e) {
                Diagnostic diag = new Diagnostic();
                diag.setSeverity(DiagnosticSeverity.Error);
                diag.setMessage("Invalid label syntax.");
                diag.setCode(DiagnosticCodes.INVALID_TARGET);
                diag.setRange(range);
                diagnostics.add(diag);
                continue;
            }

            // We're not handling external workspaces right now.
            // TODO: Handle external workspaces.
            if (label.hasWorkspace()) {
                continue;
            }

            // Convert the label to a target and ensure it exists. Use the parent of the
            // text doc because we don't want the BUILD file.
            {
                final BuildTarget target = CompatabilityUtility.labelToBuildTarget(label, textDocPath.getParent());

                boolean fileExists = false;
                Path rootPath = Workspace.getInstance().getRootFolder().getPath();
                if (label.hasPkg() && label.hasTarget()) {
                    Path pkgPath = Paths.get(label.pkg());
                    Path targetPath = Paths.get(label.target());
                    Path absPath = rootPath.resolve(pkgPath).resolve(targetPath);
                    fileExists = Files.exists(absPath);
                } else if (!label.hasPkg() && label.hasTarget()) {
                    Path pkgPath = textDocPath.getParent();
                    Path targetPath = Paths.get(label.target());
                    Path absPath = rootPath.resolve(pkgPath).resolve(targetPath);
                    fileExists = Files.exists(absPath);
                }

                boolean targetExists = api.isValidTarget(target);
                if (!fileExists && !targetExists) {
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Error);
                    diag.setCode(DiagnosticCodes.INVALID_TARGET);
                    diag.setMessage(String.format("Target '%s' does not exist.", labelStr.getValue()));
                    diag.setRange(range);
                    diagnostics.add(diag);
                    continue;
                }
            }

        }
    }
}
