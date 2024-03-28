package org.hpcclab.oprc.cli.command.cls;

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
  name = "delete",
  aliases = {"d", "rm", "remove"},
  description = "Delete a classes",
  mixinStandardHelpOptions = true
)
public class ClsDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(ClsDeleteCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;

  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @Override
  public Integer call() throws Exception {
    return webRequester.pmDeleteAndPrint(
      UriTemplate.of("/api/classes/{+cls}")
        .expandToString(Variables.variables()
          .set("cls", cls)
        ),
      commonOutputMixin.getOutputFormat()
    );
  }
}
