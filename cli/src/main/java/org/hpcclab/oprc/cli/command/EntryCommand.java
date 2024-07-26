package org.hpcclab.oprc.cli.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.hpcclab.oprc.cli.command.cls.ClsDeleteCommand;
import org.hpcclab.oprc.cli.command.cls.ClsListCommand;
import org.hpcclab.oprc.cli.command.cr.CrDeleteCommand;
import org.hpcclab.oprc.cli.command.cr.CrListCommand;
import org.hpcclab.oprc.cli.command.ctx.ContextGetCommand;
import org.hpcclab.oprc.cli.command.ctx.ContextSelectCommand;
import org.hpcclab.oprc.cli.command.ctx.ContextSetCommand;
import org.hpcclab.oprc.cli.command.dev.*;
import org.hpcclab.oprc.cli.command.fn.FnListCommand;
import org.hpcclab.oprc.cli.command.invocation.GrpcInvocationCommand;
import org.hpcclab.oprc.cli.command.invocation.InvocationCommand;
import org.hpcclab.oprc.cli.command.obj.ObjectEntryCommand;
import org.hpcclab.oprc.cli.command.pkg.PackageApplyCommand;
import org.hpcclab.oprc.cli.command.pkg.PackageDeleteCommand;
import picocli.AutoComplete;
import picocli.CommandLine.Command;

@TopCommand
@Command(
  mixinStandardHelpOptions = true,
  subcommands = {
    ObjectEntryCommand.class,
    EntryCommand.ClsEntryCommand.class,
    EntryCommand.CrEntryCommand.class,
    EntryCommand.ContextEntryCommand.class,
    EntryCommand.DevEntryCommand.class,
    EntryCommand.FnEntryCommand.class,
    EntryCommand.PackageEntryCommand.class,
    InvocationCommand.class,
    GrpcInvocationCommand.class,
    AutoComplete.GenerateCompletion.class,
  },
  description = """
    The CLI of Oparaca platform (aka OaaS).
    """,
  version = "0.2.2-SNAPSHOT"
)
public class EntryCommand {

  @Command(name = "local-develop",
    aliases = {"d", "dev"},
    description = "Emulator for local development",
    mixinStandardHelpOptions = true,
    subcommands = {
      DevPackageApplyCommand.class,
      DevPackageDeleteCommand.class,
      DevClsListCommand.class,
      DevClsDeleteCommand.class,
      DevFnListCommand.class,
      DevFnDeleteCommand.class,
      DevInvocationCommand.class,
      DevObjectCreateCommand.class,
      DevObjectCleanCommand.class,
      DevObjectFileCommand.class,
      DevObjectCatFileCommand.class,
      DevConfigCommand.class,
      DevServerCommand.class
    }
  )
  public static class DevEntryCommand {
  }

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
  public static class ClsEntryCommand {

  }

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
  public static class CrEntryCommand {

  }

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
  public static class ContextEntryCommand {

  }

  @Command(
    name = "function",
    aliases = {"fn", "f"},
    description = "Manage functions",
    mixinStandardHelpOptions = true,
    subcommands = {
      FnListCommand.class
    }
  )
  public static class FnEntryCommand {

  }

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
  public static class PackageEntryCommand {

  }


}
