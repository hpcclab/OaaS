package org.hpcclab.oprc.cli.command.invocation;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "invoke2",
  aliases = {"inv2", "i2"},
  mixinStandardHelpOptions = true
)
public class V2InvocationCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(V2InvocationCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @CommandLine.Option(names = "-c")
  String cls;
  @CommandLine.Parameters(index = "0", defaultValue = "")
  String main;
  @CommandLine.Parameters(index = "1", defaultValue = "")
  String fb;
  @CommandLine.Option(names = "--args")
  Map<String, String> args;
  @CommandLine.Option(names = {"-i", "--inputs"})
  List<String> inputs;
  @CommandLine.Option(names = {"-b", "--pipe-body"}, defaultValue = "false")
  boolean pipeBody;
  @Inject
  OutputFormatter outputFormatter;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  WebClient webClient;

  @CommandLine.Option(names = {"-a", "--async"}, defaultValue = "false")
  boolean async;

  @Override
  public Integer call() throws Exception {
    if (main == null && fb == null) {
      System.err.println("Please specify both main and fb");
      return 1;
    }
    var conf = fileManager.current();
    if (cls==null) cls = conf.getDefaultClass();
    JsonObject jsonBody = new JsonObject();
    if (pipeBody) {
      var body = System.in.readAllBytes();
      var sbody = new String(body).stripTrailing();
      jsonBody = new JsonObject(sbody);
    }
    String url;
    if (main.isEmpty()) {
      url = UriTemplate.of("{+inv}/api/classes/{cls}/invokes/{fb}")
        .expandToString(Variables.variables()
          .set("inv", conf.getInvUrl())
          .set("cls", cls)
          .set("fb", fb)
        );
    } else {
      url = UriTemplate.of("{+inv}/api/classes/{cls}/objects/{main}/invokes/{fb}")
        .expandToString(Variables.variables()
          .set("inv", conf.getInvUrl())
          .set("cls", cls)
          .set("main", main)
          .set("fb", fb)
        );
    }
    logger.debug("request to url [{}]", url);
    HttpRequest<Buffer> request;
    HttpResponse<Buffer> response;
    if (jsonBody.isEmpty()) {
      request = webClient.getAbs(url);
      request.queryParams()
        .addAll(args == null ? Map.of() : args)
        .set("_async", String.valueOf(async));
      response = request
        .sendAndAwait();
    } else {
      request = webClient.postAbs(url);
      request.queryParams()
        .addAll(args == null ? Map.of() : args)
        .set("_async", String.valueOf(async));
      response = request
        .sendJsonObjectAndAwait(jsonBody);
    }

    if (response.statusCode() >= 400) {
      if (logger.isErrorEnabled())
        logger.error("error response: {} {}", response.statusCode(), response.bodyAsString());
      return 1;
    }
    outputFormatter.print(commonOutputMixin.getOutputFormat(), response.bodyAsJsonObject());
    return 0;
  }
}
