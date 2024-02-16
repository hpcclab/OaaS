package org.hpcclab.oprc.cli.command.orbit;

import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
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
  mixinStandardHelpOptions = true
)
public class CrListCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(CrListCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;
  @Inject
  ConfigFileManager fileManager;

  @CommandLine.Parameters(defaultValue = "")
  String crId;

  @Override
  public Integer call() throws Exception {
    var oc = fileManager.current().getPmUrl();
    return webRequester.getAndPrint(
      UriTemplate.of("{+oc}/api/class-runtimes/{+crId}")
        .expandToString(Variables.variables()
          .set("oc", oc)
          .set("crId", crId)
        ),
      commonOutputMixin.getOutputFormat()
    );
  }
}
