package org.hpcclab.oprc.cli.command.invocation;

import com.jayway.jsonpath.DocumentContext;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.JsonUtil;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "invoke2",
  aliases = {"inv2", "i2"},
  description = "Invoke a function with REST API",
  mixinStandardHelpOptions = true
)
public class V2InvocationCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(V2InvocationCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @CommandLine.Option(names = "-c")
  String cls;
  @CommandLine.Option(names = {"-m", "--main"})
  String main;
  @CommandLine.Parameters(index = "0", defaultValue = "")
  String fb;
  @CommandLine.Option(names = "--args")
  Map<String, String> args = new HashMap<>();
  @CommandLine.Option(names = {"-b", "--pipe-body"}, defaultValue = "false")
  boolean pipeBody;
  @CommandLine.Option(names = {"-s", "--save"}, description = "save the object id to config file")
  boolean save;
  @Inject
  OutputFormatter outputFormatter;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  WebClient webClient;

  @CommandLine.Option(names = {"-a", "--async"}, defaultValue = "false")
  boolean async;
  @CommandLine.Option(names = {"--all"}, defaultValue = "false")
  boolean showAll;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.current();
    if (cls==null) cls = conf.getDefaultClass();
    String url;
    if (main==null)
      main = conf.getDefaultObject()==null ? "":conf.getDefaultObject();
    if (main.isBlank() && fb.isBlank()) {
      System.err.println("You must specify both main and fb");
      return 1;
    }
    if (showAll)
      args.put("_showAll", "true");
    if (main.isBlank()) {
      url = UriTemplate.of("{+inv}/api/classes/{cls}/invokes/{fb}")
        .expandToString(Variables.variables()
          .set("inv", conf.getInvUrl())
          .set("cls", cls)
          .set("fb", fb)
        );
    } else if (fb.isBlank()) {
      url = UriTemplate.of("{+inv}/api/classes/{cls}/objects/{main}")
        .expandToString(Variables.variables()
          .set("inv", conf.getInvUrl())
          .set("cls", cls)
          .set("main", main)
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
    return sendRequestAndHandle(conf, url);
  }

  private int sendRequestAndHandle(FileCliConfig.FileCliContext conf,
                                   String url) throws IOException {
    logger.debug("request to url [{}]", url);
    HttpRequest<Buffer> request;
    HttpResponse<Buffer> response;
    JsonObject jsonBody = new JsonObject();
    if (pipeBody) {
      var body = System.in.readAllBytes();
      var sbody = new String(body).stripTrailing();
      jsonBody = new JsonObject(sbody);
    }
    if (jsonBody.isEmpty()) {
      request = webClient.getAbs(url);
      request.queryParams()
        .addAll(args==null ? Map.of():args)
        .set("_async", String.valueOf(async));
      response = request
        .sendAndAwait();
    } else {
      request = webClient.postAbs(url);
      request.queryParams()
        .addAll(args==null ? Map.of():args)
        .set("_async", String.valueOf(async));
      response = request
        .sendJsonObjectAndAwait(jsonBody);
    }

    if (response.statusCode() >= 400) {
      if (logger.isErrorEnabled())
        logger.error("error response: {} {}", response.statusCode(), response.bodyAsString());
      return 1;
    }
    var respBody = response.bodyAsJsonObject();
    DocumentContext doc = JsonUtil.parse(response.bodyAsString());
    outputFormatter.print(commonOutputMixin.getOutputFormat(), respBody);
    String outId = doc.read("$.output._meta.id");
    if (save && outId != null && !outId.isEmpty()) {
      conf.setDefaultObject(outId);
      String outCLs = doc.read("$.output._meta.cls");
      conf.setDefaultClass(outCLs);
      fileManager.update(conf);
    }
    return 0;
  }
}
