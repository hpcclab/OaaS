package org.hpcclab.oprc.cli.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.hpcclab.oprc.cli.command.cls.ClsEntryCommand;
import org.hpcclab.oprc.cli.command.ctx.ContextEntryCommand;
import org.hpcclab.oprc.cli.command.fn.FnEntryCommand;
import org.hpcclab.oprc.cli.command.invocation.GrpcInvocationCommand;
import org.hpcclab.oprc.cli.command.invocation.InvocationCommand;
import org.hpcclab.oprc.cli.command.invocation.V2InvocationCommand;
import org.hpcclab.oprc.cli.command.obj.ObjectEntryCommand;
import org.hpcclab.oprc.cli.command.cr.CrEntryCommand;
import org.hpcclab.oprc.cli.command.pkg.PackageEntryCommand;
import picocli.AutoComplete;
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
    V2InvocationCommand.class,
    GrpcInvocationCommand.class,
    PackageEntryCommand.class,
    ContextEntryCommand.class,
    AutoComplete.GenerateCompletion.class
  },
  description = """
    The CLI of Oparaca platform (aka OaaS).
    """
)
public class EntryCommand {

}
