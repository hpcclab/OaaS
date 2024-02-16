package org.hpcclab.oprc.cli.command.ctx;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "get",
  aliases = "g",
  mixinStandardHelpOptions = true
)
public class ContextGetCommand  implements Callable<Integer> {


  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter formatter;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.getOrCreate();
    formatter.printObject(commonOutputMixin.getOutputFormat(), conf);
    return 0;
  }
}
