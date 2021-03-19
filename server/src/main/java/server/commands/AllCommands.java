package server.commands;

import java.util.Arrays;
import java.util.List;

/**
 * Manages a list of all command that the server can interpret and execute
 */
public class AllCommands {
    private AllCommands() {
        super();
    }

    public static final String build = "bazel.build";
    public static final String test = "bazel.test";
    public static final String none = "bazel.none";
    public static final String syncServer = "bazel.syncServer";

    public static List<String> allCommands() {
        return Arrays.asList(build, test, none, syncServer);
    }
}
