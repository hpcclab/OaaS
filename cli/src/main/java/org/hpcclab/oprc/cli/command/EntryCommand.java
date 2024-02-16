package org.hpcclab.oprc.cli.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.hpcclab.oprc.cli.command.cls.ClsEntryCommand;
import org.hpcclab.oprc.cli.command.ctx.ContextEntryCommand;
import org.hpcclab.oprc.cli.command.fn.FnEntryCommand;
import org.hpcclab.oprc.cli.command.oal.GrpcInvocationCommand;
import org.hpcclab.oprc.cli.command.oal.InvocationCommand;
import org.hpcclab.oprc.cli.command.obj.ObjectEntryCommand;
import org.hpcclab.oprc.cli.command.orbit.CrEntryCommand;
import org.hpcclab.oprc.cli.command.pkg.PackageEntryCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(
  mixinStandardHelpOptions = true,
  subcommands = {
    ObjectEntryCommand.class,
    ClsEntryCommand.class,
    CrEntryCommand.class,
    FnEntryCommand.class,
    InvocationCommand.class,
    GrpcInvocationCommand.class,
    PackageEntryCommand.class,
    ContextEntryCommand.class
  },
  description = """
    The CLI of Oparaca platform (aka OaaS).
    """
)
public class EntryCommand {

}
