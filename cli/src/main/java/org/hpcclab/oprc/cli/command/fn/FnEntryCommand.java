package org.hpcclab.oprc.cli.command.fn;

import picocli.CommandLine.Command;

@Command(
        name = "function",
        aliases = {"fn", "f"},
        mixinStandardHelpOptions = true,
        subcommands = {
                FnListCommand.class
        }
)
public class FnEntryCommand {

}
