package server.codelens;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import net.starlark.java.syntax.Expression;
import net.starlark.java.syntax.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import server.bazel.interp.Label;
import server.bazel.interp.LabelSyntaxException;
import server.commands.AllCommands;
import server.utils.Nullability;
import server.utils.StarlarkWizard;
import server.workspace.Workspace;

/**
 * This class is delegated the CodeLens functionality of the BazelServices class
 */
public class CodeLensProvider {
    private static final Logger logger = LogManager.getLogger(CodeLensProvider.class);

    private StarlarkWizard wizard;

    public CodeLensProvider() {
        this.wizard = null;
    }

    public StarlarkWizard getWizard() {
        return wizard;
    }

    public void setWizard(StarlarkWizard wizard) {
        this.wizard = wizard;
    }

    /**
     * Parses a document for actionable BUILD targets and creates codelens objects corresponding to them
     *
     * @param params the information passed to the server from the client containing the document to retrieve codelens for
     * @return a list of CodeLens objects representing the CodeLens options to be displayed to the user
     */
    public CompletableFuture<List<? extends CodeLens>> getCodeLens(CodeLensParams params) {
        final List<CodeLens> results = new ArrayList<>();

        final URI uri = URI.create(params.getTextDocument().getUri());
        final Path path = Paths.get(uri).toAbsolutePath();
        final List<StarlarkWizard.TargetMeta> targetMetas = wizard.allDeclaredTargets(uri);

        for (StarlarkWizard.TargetMeta target : targetMetas) {
            final Expression function = Nullability.nullable(() -> target.call().getFunction());
            if (function == null || function.kind() != Expression.Kind.IDENTIFIER) {
                continue;
            }

            final Identifier identifier = (Identifier) function;
            final String identifierName = identifier.getName();
            if (identifierName == null) {
                continue;
            }

            logger.info("ID NAME=" + identifierName);
            final boolean isTest = identifier.getName().endsWith("_test");
            final boolean isBinary = identifier.getName().endsWith("_binary");
            if (!isTest && !isBinary) {
                continue;
            }

            final CodeLens result = new CodeLens();

            final Range range = new Range();
            {
                final Position start = new Position();
                start.setCharacter(identifier.getStartLocation().column() - 1);
                start.setLine(identifier.getStartLocation().line() - 1);

                final Position end = new Position();
                end.setCharacter(start.getCharacter() + 999);
                end.setLine(identifier.getStartLocation().line() - 1);

                range.setStart(start);
                range.setStart(end);
            }

            final Command command = new Command();
            {
                if (isBinary) {
                    command.setTitle("Build " + target.name());
                    command.setCommand(AllCommands.build);
                } else {
                    command.setTitle("Test " + target.name());
                    command.setCommand(AllCommands.test);
                }

                String wsPath = Workspace.getInstance().getRootFolder().getPath().toAbsolutePath().toString();
                String pkgPath = path.toAbsolutePath().toString();
                if (!pkgPath.startsWith(wsPath)) {
                    continue;
                }

                pkgPath = pkgPath.substring(wsPath.length());
                if (pkgPath.startsWith("/")) {
                    pkgPath = pkgPath.substring(1);
                }
                if (pkgPath.endsWith("/")) {
                    pkgPath = pkgPath.substring(0, pkgPath.length() - 1);
                }

                final List<Object> args = new ArrayList<>();
                final Label label;
                try {
                    label = Label.fromParts(null, pkgPath, target.name().getValue());
                } catch (LabelSyntaxException e) {
                    continue;
                }

                args.add(label.value());
                command.setArguments(args);
            }

            result.setRange(range);
            result.setCommand(command);
            results.add(result);
        }

        return CompletableFuture.completedFuture(results);
    }
}
