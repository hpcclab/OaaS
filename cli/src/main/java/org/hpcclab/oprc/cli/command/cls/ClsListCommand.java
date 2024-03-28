package org.hpcclab.oprc.cli.command.cls;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
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
  description = "List classes",
  mixinStandardHelpOptions = true
)
public class ClsListCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(ClsListCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;

  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @Override
  public Integer call() throws Exception {
    return webRequester.pmGetAndPrint(
      "/api/classes/" + cls,
      commonOutputMixin.getOutputFormat()
    );
  }
}
