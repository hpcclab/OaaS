package org.hpcclab.oprc.cli.command.cr;

import picocli.CommandLine.Command;

@Command(
  name = "class-runtime",
  aliases = {"cr"},
  description = "Manage class runtimes",
  mixinStandardHelpOptions = true,
  subcommands = {
    CrListCommand.class,
    CrDeleteCommand.class
  }
)
public class CrEntryCommand {

}
