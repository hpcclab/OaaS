package org.hpcclab.oprc.cli.command.orbit;

import org.hpcclab.oprc.cli.command.cls.ClsDeleteCommand;
import org.hpcclab.oprc.cli.command.cls.ClsListCommand;
import picocli.CommandLine.Command;

@Command(
  name = "orbit",
  aliases = {"orb"},
  mixinStandardHelpOptions = true,
  subcommands = {
    OrbitListCommand.class,
    OrbitDeleteCommand.class
  }
)
public class OrbitEntryCommand {

}
