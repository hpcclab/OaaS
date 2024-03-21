package org.hpcclab.oprc.cli.command.pkg;

import picocli.CommandLine.Command;

@Command(
  name = "package",
  aliases = {"pkg", "p"},
  description = "Manage packages",
  mixinStandardHelpOptions = true,
  subcommands = {
    PackageApplyCommand.class,
    PackageDeleteCommand.class
  }
)
public class PackageEntryCommand {

}
