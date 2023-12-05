package org.hpcclab.oprc.cli.command;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import org.hpcclab.oprc.cli.command.cls.ClsEntryCommand;
import org.hpcclab.oprc.cli.command.fn.FnEntryCommand;
import org.hpcclab.oprc.cli.command.oal.InvocationCommand;
import org.hpcclab.oprc.cli.command.obj.ObjectEntryCommand;
import org.hpcclab.oprc.cli.command.pkg.PackageEntryCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(
        mixinStandardHelpOptions = true,
        subcommands = {
                ObjectEntryCommand.class,
                ClsEntryCommand.class,
                FnEntryCommand.class,
                InvocationCommand.class,
                PackageEntryCommand.class
        },
        description = """
                The CLI of Oparaca platform (aka OaaS). Before using it, you should set environment variable 'CDS_URL' to the URL of the content delivery service.
                (ex. export OPRC_CDS="http://cds.127.0.0.1.nip.io")
                """
)
public class EntryCommand {

}
