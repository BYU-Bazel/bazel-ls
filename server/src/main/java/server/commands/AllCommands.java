package server.commands;

import java.util.Arrays;
import java.util.List;

public class AllCommands {
    private AllCommands() {
        super();
    }

    public static final String build = "bazel.build";
    public static final String test = "bazel.test";
    public static final String none = "bazel.none";

    public static List<String> allCommands() {
        return Arrays.asList(build, test, none);
    }
}
