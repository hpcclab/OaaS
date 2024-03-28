package org.hpcclab.oprc.cli.command.cls;

import picocli.CommandLine.Command;

@Command(
  name = "class",
  aliases = {"cls", "c"},
  description = "Manage classes",
  mixinStandardHelpOptions = true,
  subcommands = {
    ClsListCommand.class,
    ClsDeleteCommand.class
  }
)
public class ClsEntryCommand {

}
