package server.utils;

import com.google.common.collect.ImmutableList;
import net.starlark.java.syntax.*;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for interpretting starlark files.
 */
public class StarlarkWizard {
    public StarlarkWizard() {
        super();
    }

    public ImmutableList<TargetMeta> locateTargets(StarlarkFile file) {
        final List<TargetMeta> result = new ArrayList<>();

        for (final Statement stmt : file.getStatements()) {
            if (stmt.kind() == Statement.Kind.EXPRESSION) {
                final Expression expr = ((ExpressionStatement) stmt).getExpression();
                if (expr.kind() == Expression.Kind.CALL) {
                    final TargetMeta data = new TargetMeta();

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

        return ImmutableList.copyOf(result);
    }

    public Range rangeFromExpression(Expression expr) {
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
        private StringLiteral name = null;
        private List<Expression> srcs = new ArrayList<>();
        private List<Expression> deps = new ArrayList<>();

        public StringLiteral name() {
            return name;
        }

        public Iterable<Expression> srcs() {
            return srcs;
        }

        public Iterable<Expression> deps() {
            return deps;
        }
    }
}
