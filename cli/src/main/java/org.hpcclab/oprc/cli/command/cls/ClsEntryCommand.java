package org.hpcclab.oprc.cli.command.cls;

import picocli.CommandLine.Command;

@Command(
        name = "class",
        aliases = {"cls", "c" },
        mixinStandardHelpOptions = true,
        subcommands = {
                ClsListCommand.class
        }
)
public class ClsEntryCommand {

}
