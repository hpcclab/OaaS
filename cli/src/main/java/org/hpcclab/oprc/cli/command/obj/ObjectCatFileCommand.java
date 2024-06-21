package org.hpcclab.oprc.cli.command.obj;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "cat-file",
  aliases = {"cf"},
  description = "Cat the loaded file from object",
  mixinStandardHelpOptions = true
)
public class ObjectCatFileCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(ObjectCatFileCommand.class);
  @CommandLine.Option(names = "-c")
  String cls;
  @CommandLine.Option(names = {"-m", "--main"})
  String main;
  @CommandLine.Parameters(index = "0")
  String key;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  WebClient webClient;
  @Inject
  Vertx vertx;


  @Override
  public Integer call() throws Exception {
    var conf = fileManager.current();
    if (cls==null) cls = conf.getDefaultClass();
    String url;
    if (main==null)
      main = conf.getDefaultObject();
    if (main==null) return 1;

    url = UriTemplate.of("{+inv}/api/classes/{cls}/objects/{main}/files/{key}")
      .expandToString(Variables.variables()
        .set("inv", conf.getInvUrl())
        .set("cls", cls)
        .set("main", main)
        .set("key", key)
      );
    HttpRequest<Buffer> request = webClient.getAbs(url)
      .followRedirects(true);
    HttpResponse<Buffer> response = request.sendAndAwait();
    if (response.statusCode()!=200) {
      System.err.println("statusCode: " + response.statusCode());
      return 1;
    }
    System.out.println(response.bodyAsString());
    return 0;
  }
}
