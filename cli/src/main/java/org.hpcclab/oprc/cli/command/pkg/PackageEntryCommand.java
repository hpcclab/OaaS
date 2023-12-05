package org.hpcclab.oprc.cli.command.pkg;

import picocli.CommandLine.Command;

@Command(
        name = "package",
        aliases = {"pkg", "p"},
        mixinStandardHelpOptions = true,
        subcommands = {
                PackageApplyCommand.class
        }
)
public class PackageEntryCommand {

}
