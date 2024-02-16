package org.hpcclab.oprc.cli.command.ctx;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "set",
  aliases = {"s", "update"},
  mixinStandardHelpOptions = true
)
public class ContextSetCommand implements Callable<Integer> {

  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter formatter;
  @CommandLine.Parameters(defaultValue = "")
  String ctx;

  @CommandLine.Option(
    names = {"--pm"},
    description = "Base URL of oc server. Default(ENV:OCLI_PM): ${DEFAULT-VALUE}",
    defaultValue = "${env:OCLI_PM}"
  )
  String pmUrl;

  @CommandLine.Option(
    names = {"--inv"},
    description = "Base URL of invoker server. Default(ENV:OCLI_INVOKER): ${DEFAULT-VALUE}",
    defaultValue = "${env:OCLI_INVOKER}"
  )
  String invUrl;

  @CommandLine.Option(
    names = {"--proxy"},
    description = "The URL of proxy server. Default(ENV:OCLI_PROXY): ${DEFAULT-VALUE}",
    defaultValue = "${env:OCLI_PROXY}"
  )
  String proxy;
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.getOrCreate();
    if (ctx==null) ctx = conf.getCurrentContext();
    var ctxConf = conf.getContexts().get(ctx);
    if (ctxConf==null) ctxConf = conf.current().toBuilder().build();
    if (proxy!=null) {
      ctxConf.setProxy(proxy);
    }
    if (invUrl!=null) {
      ctxConf.setInvUrl(invUrl);
    }
    if (pmUrl!=null) {
      ctxConf.setPmUrl(pmUrl);
    }
    conf.getContexts().put(ctx, ctxConf);
    fileManager.update(conf);
    formatter.printObject(commonOutputMixin.getOutputFormat(), conf);
    return 0;
  }
}
