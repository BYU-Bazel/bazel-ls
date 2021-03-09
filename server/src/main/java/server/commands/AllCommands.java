package server.commands;

import java.util.Arrays;
import java.util.List;

public class AllCommands {
    private AllCommands() {
        super();
    }

    public static List<String> allCommands() {
        return Arrays.asList("bazel.none");
    }
}
