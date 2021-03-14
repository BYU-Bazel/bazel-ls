package server.commands;

import server.bazel.cli.AbstractBazelCommand;

public class CommandToRun extends AbstractBazelCommand {
    protected CommandToRun(String cmd, String target) {
        super(String.format("%s %s", cmd, target));
    }
}