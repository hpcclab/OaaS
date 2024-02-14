package org.hpcclab.oprc.cli.command.orbit;

import picocli.CommandLine.Command;

@Command(
  name = "class-runtime",
  aliases = {"cr"},
  mixinStandardHelpOptions = true,
  subcommands = {
    CrListCommand.class,
    CrDeleteCommand.class
  }
)
public class CrEntryCommand {

}
