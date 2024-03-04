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
  @Inject
  ConfigFileManager fileManager;
  @CommandLine.Parameters(defaultValue = "")
  String fn;


  @Override
  public Integer call() throws Exception {
    FileCliConfig.FileCliContext fileCliContext = fileManager.current();
    var pm = fileCliContext.getPmUrl();
    return webRequester.getAndPrint(
      UriTemplate.of("{+pm}/api/functions/{+fn}")
        .expandToString(Variables.variables()
          .set("pm", pm)
          .set("fn", fn)
        ),
      fileCliContext.getPmVirtualHost(),
      commonOutputMixin.getOutputFormat()
    );
  }
}
