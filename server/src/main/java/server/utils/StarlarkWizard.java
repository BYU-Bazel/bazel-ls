package server.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.starlark.java.syntax.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for interpretting starlark files.
 */
public class StarlarkWizard {
    private static final Logger logger = LogManager.getLogger(StarlarkWizard.class);

    private final DocumentTracker tracker;
    private final Map<URI, StarlarkFile> files;

    public StarlarkWizard(DocumentTracker tracker) {
        super();
        Preconditions.checkNotNull(tracker);
        this.tracker = tracker;
        files = new HashMap<>();
    }

    public DocumentTracker getTracker() {
        return tracker;
    }

    public boolean containsFile(URI uri) {
        return files.containsKey(uri);
    }

    public StarlarkFile getFile(URI uri) {
        Preconditions.checkNotNull(uri);

        if (files.containsKey(uri)) {
            return files.get(uri);
        }

        return syncFile(uri);
    }

    public StarlarkFile syncFile(URI uri) {
        Preconditions.checkNotNull(uri);

        final String content = getTracker().getContents(uri);
        if (content == null) {
            return null;
        }

        try {
            final ParserInput input = ParserInput.fromString(content, uri.getPath());
            final StarlarkFile file = StarlarkFile.parse(input);
            files.put(uri, file);
            return file;
        } catch (Error | RuntimeException e) {
            logger.error("Parsing failed for an unknown reason!");
            logger.error(Logging.stackTraceToString(e));
            return null;
        }
    }

    public void clearFiles() {
        files.clear();
    }

    public ImmutableList<TargetMeta> allDeclaredTargets(URI uri) {
        final List<TargetMeta> result = new ArrayList<>();

        final StarlarkFile file = getFile(uri);
        if (file == null) {
            logger.info("Unable to get file for declared targets.");
            return ImmutableList.of();
        }

        for (final Statement stmt : file.getStatements()) {
            if (stmt.kind() == Statement.Kind.EXPRESSION) {
                final Expression expr = ((ExpressionStatement) stmt).getExpression();
                if (expr.kind() == Expression.Kind.CALL) {
                    final TargetMeta data = new TargetMeta();

                    // Create mapping of all named parameters.
                    final CallExpression call = (CallExpression) expr;
                    data.call = call;

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
                        data.srcsArg = listExpr;
                    }

                    // Locate all srcs that are lists. Treat each element as a label.
                    if (callArgs.containsKey("deps") && callArgs.get("deps").kind() == Expression.Kind.LIST_EXPR) {
                        final ListExpression listExpr = (ListExpression) callArgs.get("deps");
                        data.deps.addAll(listExpr.getElements());
                        data.depsArg = listExpr;
                    }

                    // Cache this as a valid target meta data item.
                    result.add(data);
                }
            }
        }

        return ImmutableList.copyOf(result);
    }

    public boolean targetWithNameExists(URI uri, String name) {
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(name);

        final ImmutableList<TargetMeta> targets = allDeclaredTargets(uri);
        for (TargetMeta meta : targets) {
            final String metaName = meta.name().getValue();
            if (metaName == null) {
                continue;
            }

            if (metaName.equals(name)) {
                return true;
            }
        }

        return false;
    }

    public boolean anyCallsContainPos(URI uri, Position pos) {
        Preconditions.checkNotNull(uri);
        Preconditions.checkNotNull(pos);

        final ImmutableList<TargetMeta> targets = allDeclaredTargets(uri);
        if (targets == null) {
            return false;
        }

        for (final TargetMeta target : targets) {
            if (target.call != null && callExpressionContainsPos(target.call, pos)) {
                return true;
            }
        }

        return false;
    }

    private static boolean callExpressionContainsPos(CallExpression expression, Position pos) {
        Preconditions.checkNotNull(expression);
        Preconditions.checkNotNull(pos);

        final int startRow = expression.getStartLocation().line() - 1;
        final int endRow = expression.getEndLocation().line() - 1;

        final int startCol = expression.getStartLocation().column() - 1;
        final int endCol = expression.getEndLocation().column() - 1;

        if (pos.getLine() < startRow || pos.getLine() > endRow) {
            return false;
        }

        if (pos.getLine() == startRow && pos.getCharacter() < startCol) {
            return false;
        }

        return pos.getLine() != endRow || pos.getCharacter() <= endCol;
    }

    public static Range rangeFromExpression(Expression expr) {
        final int line = expr.getStartLocation().line() - 1;
        final int colstart = expr.getStartLocation().column();

        final int colend;
        if (expr.kind() == Expression.Kind.STRING_LITERAL) {
            StringLiteral literal = (StringLiteral) expr;
            colend = colstart + literal.getValue().length();
        } else {
            colend = expr.getEndLocation().column();
        }

        return new Range(new Position(line, colstart), new Position(line, colend));
    }

    public static class TargetMeta {
        private CallExpression call = null;
        private StringLiteral name = null;
        private ListExpression srcsArg = null;
        private ListExpression depsArg = null;
        private List<Expression> srcs = new ArrayList<>();
        private List<Expression> deps = new ArrayList<>();

        public ListExpression depsArg() {
            return depsArg;
        }

        public ListExpression srcsArg() {
            return srcsArg;
        }

        public StringLiteral name() {
            return name;
        }

        public Iterable<Expression> srcs() {
            return srcs;
        }

        public Iterable<Expression> deps() {
            return deps;
        }

        public CallExpression call() {
            return call;
        }
    }
}
