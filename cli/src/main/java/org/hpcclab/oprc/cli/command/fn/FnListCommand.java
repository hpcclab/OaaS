package org.hpcclab.oprc.cli.command.fn;

import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.WebRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "list",
  aliases = {"l"},
  mixinStandardHelpOptions = true
)
public class FnListCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(FnListCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;
  @CommandLine.Parameters(defaultValue = "")
  String fn;

  @Override
  public Integer call() throws Exception {
    return webRequester.pmGetAndPrint(
      "/api/functions/" + fn,
      commonOutputMixin.getOutputFormat()
    );
  }
}
