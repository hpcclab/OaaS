package org.hpcclab.oprc.cli.command.ctx;

import org.hpcclab.oprc.cli.command.obj.ObjectCreateCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
  name = "context",
  aliases = {"ctx"},
  description = "Manage contexts",
  mixinStandardHelpOptions = true,
  subcommands = {
    ContextGetCommand.class,
    ContextSelectCommand.class,
    ContextSetCommand.class
  }
)
public class ContextEntryCommand {

}
