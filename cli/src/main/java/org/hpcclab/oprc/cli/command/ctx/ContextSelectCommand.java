package org.hpcclab.oprc.cli.command.ctx;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "select",
  aliases = {"use", "u"},
  mixinStandardHelpOptions = true
)
public class ContextSelectCommand implements Callable<Integer> {

  @Inject
  ConfigFileManager fileManager;
  @CommandLine.Parameters()
  String ctx;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.getOrCreate();
    var old = conf.getCurrentContext();
    if (!conf.getContexts().containsKey(ctx)) {
      System.err.printf("No context with name '%s' is defined%n", ctx);
      return 1;
    }
    conf.setCurrentContext(ctx);
    fileManager.update(conf);
    System.out.printf("Done changing current context from '%s' to '%s'.%n", old, ctx);
    return 0;
  }
}
