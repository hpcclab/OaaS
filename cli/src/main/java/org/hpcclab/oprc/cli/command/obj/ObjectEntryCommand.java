package org.hpcclab.oprc.cli.command.obj;

import picocli.CommandLine.Command;

@Command(
        name = "object",
        aliases = {"obj", "o"},
        mixinStandardHelpOptions = true,
        subcommands = {
                ObjectCreateCommand.class
        }
)
public class ObjectEntryCommand {

}
