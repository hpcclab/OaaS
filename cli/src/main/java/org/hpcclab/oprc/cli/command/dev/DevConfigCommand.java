package org.hpcclab.oprc.cli.command.dev;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "config",
  aliases = "conf",
  description = "Manage local development configurations",
  mixinStandardHelpOptions = true
)
public class DevConfigCommand implements Callable<Integer> {


  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @CommandLine.Option(names = { "-r", "--reset",})
  boolean reset;

  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter formatter;

  @Override
  public Integer call() throws Exception {
    FileCliConfig config = fileManager.getOrCreate();
    var localDev = config.getLocalDev();
    if (localDev == null || reset) {
      localDev = fileManager.getDefault().getLocalDev();
      config.setLocalDev(localDev);
      fileManager.update(config);
    }
    formatter.printObject(commonOutputMixin.getOutputFormat(), localDev);
    return 0;
  }
}
