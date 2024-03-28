package org.hpcclab.oprc.cli.command.cr;

import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.WebRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "delete",
  aliases = {"d", "remove", "rm"},
  mixinStandardHelpOptions = true
)
public class CrDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(CrDeleteCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @Inject
  WebRequester webRequester;

  @CommandLine.Parameters(defaultValue = "")
  String crId;

  @Override
  public Integer call() throws Exception {
    return webRequester.pmDeleteAndPrint(
      UriTemplate.of("/api/class-runtimes/{+crId}")
        .expandToString(Variables.variables()
          .set("crId", crId)
        ),
      commonOutputMixin.getOutputFormat()
    );
  }
}
