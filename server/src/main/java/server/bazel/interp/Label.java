package server.bazel.interp;

import server.utils.Nullability;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Bazel target label. E.g. `@maven//some/other:package_name`.
 */
public class Label {
    private final LabelWorkspace workspace;
    private final boolean root;
    private final LabelPkg pkg;
    private final LabelName name;

    private Label(LabelWorkspace workspace, boolean root, LabelPkg pkg, LabelName name) {
        this.workspace = workspace;
        this.root = root;
        this.pkg = pkg;
        this.name = name;
    }

    /**
     * Factory to parse labels from a string.. The value could be similar to any
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
        final String workspaceRegex = "(?:@([a-zA-Z0-9._-]+))";
        final String rootRegex = "(//)";
        final String pkgRegex = "([a-zA-Z0-9._-]+(?:/[a-zA-Z0-9._-]+)*)";
        final String nameRegex = "(?::([a-zA-Z0-9._-]+))";
        final String fullRegex = String.format("^%s?%s?%s?%s?$", workspaceRegex, rootRegex, pkgRegex, nameRegex);

        // Capturing Groups:
        // 0: Entire label string value.
        // 1: Workspace name (can be empty).
        // 1: Root which represents "//" (can be empty).
        // 2: Package (can be empty).
        // 3: Name of rule (can be empty).
        final Pattern pattern = Pattern.compile(fullRegex);
        final Matcher matcher = pattern.matcher(value);

        // Construct a label from the capturing groups.
        Label label;
        if (matcher.find()) {
            String workspaceValue = Nullability.nullableOr("", () -> matcher.group(1));
            String rootValue = Nullability.nullableOr("", () -> matcher.group(2));
            String pkgValue = Nullability.nullableOr("", () -> matcher.group(3));
            String nameValue = Nullability.nullableOr("", () -> matcher.group(4));

            label = new Label(
                    workspaceValue.isEmpty() ? null : LabelWorkspace.fromString(workspaceValue),
                    !rootValue.isEmpty(),
                    pkgValue.isEmpty() ? null : LabelPkg.fromString(pkgValue),
                    nameValue.isEmpty() ? null : LabelName.fromString(nameValue));
        } else {
            throw new LabelSyntaxException();
        }

        // Labels in the shape of `@//:` or `@:` are not supported.
        if (!label.hasWorkspace() && !label.hasPkg() && !label.hasName()) {
            throw new LabelSyntaxException("A label may not be empty.");
        }

        // Labels that don't have a root could be source file. A source file is valid
        // iff it is referencing a file relative to wherever its declaration is in the
        // file tree. Referencing sub files within that directory is not valid.
        if (!label.hasWorkspace() && !label.hasRoot() && !label.hasName() && label.pkg().value().contains("/")) {
            throw new LabelSyntaxException("A source file may not contain any \"/\" characters.");
        }

        return label;
    }

    /**
     * If a label is local, it means that it is referencing something within the same
     * BUILD file. E.g. if a label is `:target_name`, and resides in a BUILD file at
     * `/some/package/BUILD`, then that label would be referencing a label at the Bazel
     * location `//some/package:target_name`.
     *
     * @return Whether this label is a local reference.
     */
    public boolean isLocal() {
        return !hasWorkspace() && !hasRoot() && !hasPkg() && hasName();
    }

    /**
     * Returns whether or not this is a source file reference. A label may only be a
     * source file if it does not have a root, a workspace, or a name. The source file
     * value will effectively be the of the package value (pkg).
     *
     * @return Whether this label represents a source file.
     */
    public boolean isSourceFile() {
        return !hasRoot() && !hasWorkspace() && hasPkg() && !hasName();
    }

    /**
     * The workspace that this label resides in. For example, if a project depended
     * on a rule from a Maven workspace `@maven//some/other:package`, then this
     * parameter would be equal to `maven`.
     * <p>
     * If empty, this rule is a part of the current workspace.
     *
     * @return The workspace.
     */
    public LabelWorkspace workspace() {
        return workspace;
    }

    /**
     * @return Whether the workspace field is declared in this label.
     */
    public boolean hasWorkspace() {
        return workspace() != null;
    }

    /**
     * The root value. This will be the the `//` immediately following a workspace
     * or an implied workspace name. This may be left out, in which case this label
     * could represent a source file or local rull dependency.
     *
     * @return Whether the root field is declared in this label.
     */
    public boolean hasRoot() {
        return root;
    }

    /**
     * The package relative to the workspace file. For example, if a project depended
     * on a rule from a Maven workspace `@maven//some/other:package`, then this
     * parameter would be equal to `some/other`.
     *
     * @return The package.
     */
    public LabelPkg pkg() {
        return pkg;
    }

    /**
     * @return Whether the package field is declared in this label.
     */
    public boolean hasPkg() {
        return pkg() != null;
    }

    /**
     * The name of the label. This will be the name provided on the declaring rule.
     * For example, if a project depended on a rule from a Maven workspace
     * `@maven//some/other:package_name`, then this parameter would be equal to
     * `package_name`.
     *
     * @return The name.
     */
    public LabelName name() {
        return name;
    }

    /**
     * @return Whether the name field is declared in this label.
     */
    public boolean hasName() {
        return name() != null;
    }

    /**
     * Converts this label into its string literal form. An example of a string
     * literal form would be `@maven//path/to:package`. Assumes that this label
     * has been correctly instantiated.
     *
     * @return A string literal label value.
     */
    public String value() {
        final StringBuilder builder = new StringBuilder();

        // Append the "@workspace" if specified.
        if (hasWorkspace()) {
            builder.append(String.format("@%s", workspace()));
        }

        // Append the "//" if specified.
        if (hasRoot()) {
            builder.append("//");
        }

        // Append the "path/to/package" if specified.
        if (hasPkg()) {
            builder.append(pkg());
        }

        // Append the ":name_of_package" if specified.
        if (hasName()) {
            builder.append(String.format(":%s", name()));
        }

        return builder.toString();
    }

    // TODO(josiah): Create a way to map from any label to a label's full path.
    // public String absolute(RepositoryMapping mapping) {
    //    given :something, return @full//path/to:something
    // }

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
                Objects.equals(root, label.root) &&
                Objects.equals(pkg, label.pkg) &&
                Objects.equals(name, label.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspace, root, pkg, name);
    }
}
