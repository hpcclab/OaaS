package org.hpcclab.oprc.cli.command.fn;

import picocli.CommandLine.Command;

@Command(
  name = "function",
  aliases = {"fn", "f"},
  description = "Manage functions",
  mixinStandardHelpOptions = true,
  subcommands = {
    FnListCommand.class
  }
)
public class FnEntryCommand {

}
