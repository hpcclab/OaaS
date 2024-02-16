package org.hpcclab.oprc.cli.command.cls;

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
  name = "delete",
  aliases = {"d", "rm", "remove"},
  mixinStandardHelpOptions = true
)
public class ClsDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(ClsDeleteCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;
  @Inject
  ConfigFileManager fileManager;

  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @Override
  public Integer call() throws Exception {
    var oc = fileManager.current().getPmUrl();
    return webRequester.deleteAndPrint(
      UriTemplate.of("{+oc}/api/classes/{+cls}")
        .expandToString(Variables.variables()
          .set("oc", oc)
          .set("cls", cls)
        ),
      commonOutputMixin.getOutputFormat()
    );
  }
}
