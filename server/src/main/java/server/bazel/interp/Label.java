package server.bazel.interp;

import com.google.common.base.Preconditions;
import server.utils.Nullability;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Bazel target label. E.g. `@maven//some/other:package_name`.
 */
public class Label {
    private final String workspace;
    private final String pkg;
    private final String target;

    private Label(String workspace, String pkg, String target) {
        this.workspace = workspace;
        this.pkg = pkg;
        this.target = target;
    }

    /**
     * Factory to parse labels from a string. The value could be similar to any
     * of the following forms.
     * <p>
     * :name
     * //foo/bar
     * //foo/bar:quux
     * {@literal @}foo
     * {@literal @}foo//bar
     * {@literal @}foo//bar:baz
     *
     * @param value The value to parse.
     * @return The parsed value in Label form.
     * @throws LabelSyntaxException If the string is not parsable.
     */
    public static Label parse(String value) throws LabelSyntaxException {
        final String workspaceRegex = "(?:@([^\\/:]+))";
        final String rootRegex = "(//)";
        final String pkgRegex = "([^\\/:]*(?:/[^\\/:]+)*)";
        final String targetRegex = "([^\\/:]+(?:/[^\\/:]+)*)";
        final String fullRegex = String.format("^%s?(?:%s%s)?(?::?%s)?$", workspaceRegex, rootRegex,
                pkgRegex, targetRegex);

        // Capturing Groups:
        // 0: Entire label string value.
        // 1: Workspace name (can be empty).
        // 2: Root indicator, e.g. "//" (can be empty).
        // 3: Package path (can be empty).
        // 4: Target name of rule (can be empty).
        final Pattern pattern = Pattern.compile(fullRegex);
        final Matcher matcher = pattern.matcher(value);

        // Construct a label from the capturing groups.
        if (!matcher.find()) {
            throw new LabelSyntaxException("Invalid label syntax.");
        }

        final String workspaceValue = matcher.group(1);
        final String rootValue = matcher.group(2);
        final String pkgValue = matcher.group(3);
        final String targetValue = matcher.group(4);

        final boolean hasWorkspace = workspaceValue != null;
        final boolean hasRoot = rootValue != null;
        final boolean hasPkg = pkgValue != null;
        final boolean hasTarget = targetValue != null;

        // An empty label is not a label at all. E.g. //: is not valid
        if (!hasWorkspace && !hasPkg && !hasTarget) {
            throw new LabelSyntaxException("A label may not be empty.");
        }

        return new Label(
                hasWorkspace ? workspaceValue : null,
                hasRoot ? Nullability.nullableOr("", () -> pkgValue) : null,
                hasTarget ? targetValue : null
        );
    }

    /**
     * Converts this label into a path. External workspaces are not supported at the moment.
     *
     * @param input Context by which to resolve this label.
     * @return The resolved label result.
     * @throws LabelNotFoundException If the path of this label couldn't be resolved.
     */
    public LabelResolveOutput resolve(LabelResolveInput input) throws LabelNotFoundException {
        Preconditions.checkNotNull(input.getLocalWorkspacePath());
        Preconditions.checkNotNull(input.getLocalDeclaringFilePath());
        Preconditions.checkNotNull(input.getFileRepository());

        final LabelResolveOutput output = new LabelResolveOutput();

        // Only local workspaces are supported at the moment.
        final Path workspacePath;
        if (hasWorkspace()) {
            throw new UnsupportedOperationException();
        } else {
            workspacePath = input.getLocalWorkspacePath().toAbsolutePath();
        }

        // The path to the declaring package. Local labels are special. The base path
        // must be inferred based on the path of the current package. If the label isn't
        // local, then hand the path absolutely.
        final Path pkgPath;
        if (isLocal()) {
            String temp = input.getLocalDeclaringFilePath().getParent().toAbsolutePath().toString();
            temp = temp.substring(workspacePath.toString().length());
            pkgPath = Paths.get(temp);
        } else if (hasPkg()) {
            pkgPath = Paths.get(pkg());
        } else {
            pkgPath = Paths.get("/");
        }

        // The path to the target within the package. This could be empty if the target
        // is to be inferred from the package path.
        final Path targetPath;
        if (hasTarget()) {
            targetPath = Paths.get(target());
        } else {
            targetPath = Paths.get("/");
        }

        // Resolve the full path.
        {
            Path resolved = workspacePath.resolve(pkgPath).resolve(targetPath);
            resolved = input.getFileRepository().getFileSystem().getPath(resolved.toString());
            if (input.getFileRepository().isFile(resolved)) {
                output.setPath(resolved);
                return output;
            }
        }

        // Resolve the path as an inferred build file.
        {
            final Path resolved = workspacePath.resolve(pkgPath);
            final Path buildResolved = resolved.resolve(Paths.get("BUILD")).toAbsolutePath();
            final Path buildBazelResolved = resolved.resolve(Paths.get("BUILD.bazel")).toAbsolutePath();

            if (input.getFileRepository().isFile(buildResolved)) {
                output.setPath(buildResolved);
                return output;
            }

            if (input.getFileRepository().isFile(buildBazelResolved)) {
                output.setPath(buildBazelResolved);
                return output;
            }
        }

        throw new LabelNotFoundException("Unable to resolve label path.");
    }

    /**
     * If a label is local, it means that it is referencing something within the same
     * package. E.g. if a label is `:target_name`, and resides in a BUILD file at
     * `/some/package/BUILD`, then that label would be referencing a label at the Bazel
     * location `//some/package:target_name`.
     *
     * @return Whether this label is a local reference.
     */
    public boolean isLocal() {
        return !hasWorkspace() && !hasPkg() && hasTarget();
    }

    /**
     * @return Whether the workspace field is declared in this label.
     */
    public boolean hasWorkspace() {
        return workspace() != null;
    }

    /**
     * The workspace that this label resides in. For example, if a project depended
     * on a rule from a Maven workspace `@maven//some/other:package`, then the value
     * of WorkspaceID would be equal to `maven`.
     * <p>
     * If empty, this rule is a part of the current workspace.
     *
     * @return The workspace.
     */
    public String workspace() {
        return workspace;
    }

    /**
     * @return Whether the package field is declared in this label. This is defined
     * by the existence of a `//` after the workspace name.
     */
    public boolean hasPkg() {
        return pkg() != null;
    }

    /**
     * The package relative to the workspace file. For example, if a project depended
     * on a rule from a Maven workspace `@maven//some/other:package`, then the value
     * of the PkgID would be equal to `some/other`.
     *
     * @return The package.
     */
    public String pkg() {
        return pkg;
    }

    /**
     * @return Whether the target field is declared in this label.
     */
    public boolean hasTarget() {
        return target() != null;
    }

    /**
     * The target of the label. This will be the target provided on the declaring rule.
     * For example, if a project depended on a rule from a Maven workspace
     * `@maven//some/other:package_name`, then the value of the target would be equal
     * to `package_name`.
     *
     * @return The target.
     */
    public String target() {
        return target;
    }

    /**
     * Converts this label into its string literal form. An example of a string literal
     * form would be `@maven//path/to:package`. Assumes that this label has been correctly
     * instantiated.
     *
     * @return A string literal label value.
     */
    public String value() {
        final StringBuilder builder = new StringBuilder();

        // Append the "@workspace" if specified.
        if (hasWorkspace()) {
            builder.append("@");
            builder.append(workspace());
        }

        // Append the "//path/to/package" if specified.
        if (hasPkg()) {
            builder.append("//");
            builder.append(pkg());
        }

        // Append the ":name_of_package" if specified.
        if (hasTarget()) {
            builder.append(":");
            builder.append(target());
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        Label label = (Label) o;
        return Objects.equals(workspace, label.workspace) &&
                Objects.equals(pkg, label.pkg) &&
                Objects.equals(target, label.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, pkg, target);
    }
}
