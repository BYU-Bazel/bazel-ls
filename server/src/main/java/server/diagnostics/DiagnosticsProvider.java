package server.diagnostics;

import com.google.common.base.Preconditions;
import net.starlark.java.syntax.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.*;
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
import java.util.*;

// TODO: This should be the analysis stuff.

/**
 * Provides diagnostics for a given bazel file. Note, this class is purely a prototype.
 * This class will be overruled when we figure out how to do generic Bazel interpretting.
 */
public class DiagnosticsProvider {
    private static final Logger logger = LogManager.getLogger(DiagnosticsProvider.class);

    private Path textDocPath;

    public DiagnosticsProvider() {
        super();
    }

    private List<TargetMetaData> locateTargetMetaData(StarlarkFile file) {
        final List<TargetMetaData> result = new ArrayList<>();

        for (final Statement stmt : file.getStatements()) {
            if (stmt.kind() == Statement.Kind.EXPRESSION) {
                final Expression expr = ((ExpressionStatement) stmt).getExpression();
                if (expr.kind() == Expression.Kind.CALL) {
                    final TargetMetaData data = new TargetMetaData();

                    // Create mapping of all named parameters.
                    final CallExpression call = (CallExpression) expr;
                    final Map<String, Expression> callArgs = new HashMap<>();
                    for (final Argument arg : call.getArguments()) {
                        if (arg.getName() == null) {
                            continue;
                        }

                        callArgs.put(arg.getName(), arg.getValue());
                    }

                    // A target must have a name.
                    if (callArgs.containsKey("name") && callArgs.get("name").kind() == Expression.Kind.STRING_LITERAL) {
                        data.name = (StringLiteral) callArgs.get("name");
                    } else {
                        continue;
                    }

                    // Locate all srcs that are lists. Treat each element as a label.
                    if (callArgs.containsKey("srcs") && callArgs.get("srcs").kind() == Expression.Kind.LIST_EXPR) {
                        final ListExpression listExpr = (ListExpression) callArgs.get("srcs");
                        data.srcs.addAll(listExpr.getElements());
                    }

                    // Locate all srcs that are lists. Treat each element as a label.
                    if (callArgs.containsKey("deps") && callArgs.get("deps").kind() == Expression.Kind.LIST_EXPR) {
                        final ListExpression listExpr = (ListExpression) callArgs.get("deps");
                        data.deps.addAll(listExpr.getElements());
                    }

                    // Cache this as a valid target meta data item.
                    result.add(data);
                }
            }
        }

        return result;
    }

    private Range rangeFromExpression(Expression expr) {
        final int line = expr.getStartLocation().line() - 1;
        final int colstart = expr.getStartLocation().column();
        final int colend = expr.getEndLocation().column();
        return new Range(new Position(line, colstart), new Position(line, colend));
    }

    private List<Diagnostic> getDiagnosticsForLabelList(List<Expression> expressions) {
        final WorkspaceTree tree = Workspace.getInstance().getWorkspaceTree();
        final WorkspaceAPI api = new WorkspaceAPI(tree);

        final List<Diagnostic> diagnostics = new ArrayList<>();
        final Set<String> labelsInList = new HashSet<>();

        for (final Expression expr : expressions) {
            final Range range = rangeFromExpression(expr);

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

            // This instance is a string and could potentially be a valid label.
            final String labelStr = ((StringLiteral) expr).getValue();
            final Label label;

            // Validate the label syntax.
            try {
                label = Label.parse(labelStr);
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
                    diag.setMessage(String.format("Target '%s' does not exist.", labelStr));
                    diag.setRange(range);
                    diagnostics.add(diag);
                    continue;
                }
            }

            // Notify the user about duplicate labels.
            {
                if (labelsInList.contains(labelStr)) {
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Warning);
                    diag.setCode(DiagnosticCodes.DUPLICATE_TARGET);
                    diag.setMessage(String.format("Duplicate label '%s' found in label list.", labelStr));
                    diag.setRange(range);
                    diagnostics.add(diag);
                    continue;
                }

                labelsInList.add(labelStr);
            }
        }

        return diagnostics;
    }

    public void handleDiagnostics(DiagnosticParams params) {
        logger.info(String.format("Handling diagnostics for params:\n%s", params));

        Preconditions.checkNotNull(params);
        Preconditions.checkNotNull(params.getClient());
        Preconditions.checkNotNull(params.getTracker());
        Preconditions.checkNotNull(params.getUri());

        final URI textDocURI = params.getUri();
        textDocPath = Paths.get(textDocURI);
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

        // Interpret all targets and gather information for diagnostics.
        final List<TargetMetaData> targetMetaData = locateTargetMetaData(file);
        final Map<String, StringLiteral> allTargetNames = new HashMap<>();
        for (final TargetMetaData data : targetMetaData) {
            // Add all diagnostics for srcs attributes.
            {
                final List<Diagnostic> srcDiagnostics = getDiagnosticsForLabelList(data.srcs);
                diagnostics.addAll(srcDiagnostics);
            }

            // Add all diagnostics for deps attributes.
            {
                final List<Diagnostic> srcDiagnostics = getDiagnosticsForLabelList(data.deps);
                diagnostics.addAll(srcDiagnostics);
            }

            // Track duplicate target names.
            {
                final String nameValue = data.name.getValue();

                if (allTargetNames.containsKey(nameValue)) {
                    Diagnostic diag = new Diagnostic();
                    diag.setSeverity(DiagnosticSeverity.Warning);
                    diag.setCode(DiagnosticCodes.DUPLICATE_TARGET);
                    diag.setMessage(String.format("Duplicate target '%s' found in file.", nameValue));
                    diag.setRange(rangeFromExpression(data.name));
                    diagnostics.add(diag);
                    continue;
                }

                allTargetNames.put(nameValue, data.name);
            }
        }

        // Publish all diagnostics.
        final PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
        diagnosticsParams.setUri(params.getUri().toString());
        diagnosticsParams.setDiagnostics(diagnostics);
        params.getClient().publishDiagnostics(diagnosticsParams);
    }

    private static class TargetMetaData {
        public StringLiteral name = null;
        public List<Expression> srcs = new ArrayList<>();
        public List<Expression> deps = new ArrayList<>();
    }
}
