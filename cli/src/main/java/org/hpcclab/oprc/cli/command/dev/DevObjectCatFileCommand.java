package org.hpcclab.oprc.cli.command.dev;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "object-cat-file",
  aliases = {"ocf"},
  description = "Cat the loaded file from object",
  mixinStandardHelpOptions = true
)
public class DevObjectCatFileCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevObjectCatFileCommand.class);
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
  InvocationManager invocationManager;


  @Override
  public Integer call() throws Exception {
    var conf = fileManager.dev();
    if (cls==null) cls = conf.getDefaultClass();
    if (main==null)
      main = conf.getDefaultObject();
    if (main==null) return 1;
    InvocationReqHandler reqHandler = invocationManager.getReqHandler();
    InvocationRequest req = InvocationRequest.builder()
      .cls(cls)
      .fb("file")
      .main(main)
      .args(Map.of("key", key))
      .build();
    InvocationResponse resp = reqHandler.invoke(req).await().indefinitely();
    String urlToLoad = resp.body().getNode().get(key).asText();
    HttpRequest<Buffer> request = webClient.getAbs(urlToLoad)
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
