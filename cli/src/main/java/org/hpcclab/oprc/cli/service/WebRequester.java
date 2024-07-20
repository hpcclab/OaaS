package org.hpcclab.oprc.cli.service;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.conf.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ApplicationScoped
public class WebRequester {
  private static final Logger logger = LoggerFactory.getLogger(WebRequester.class);
  @Inject
  WebClient webClient;
  @Inject
  OutputFormatter outputFormatter;
  @Inject
  ConfigFileManager configFileManager;


  public JsonObject request(HttpMethod method, String url) {
    return request(method, url, null);
  }

  public JsonObject request(HttpMethod method, String url, String virtualHost) {
    HttpRequest<Buffer> request = webClient.requestAbs(method, url);
    if (virtualHost!=null) request.virtualHost(virtualHost);
    var res = request
      .sendAndAwait();
    if (res.statusCode()>299) {
      logger.error("error response: code={}, body={}",
        res.statusCode(),
        res.bodyAsString());
      return null;
    }
    return res.bodyAsJsonObject();
  }


  public int getAndPrint(String url,
                         String overrideHost,
                         OutputFormat format) {
    var jsonObject = request(HttpMethod.GET, url, overrideHost);
    if (jsonObject==null)
      return 1;
    outputFormatter.print(format, jsonObject);
    return 0;
  }

  public int pmGetAndPrint(String path,
                           OutputFormat format) throws IOException {
    FileCliConfig.FileCliContext fileCliContext = configFileManager.current();
    String pmUrl = fileCliContext.getPmUrl();
    var jsonObject = request(HttpMethod.GET, pmUrl + path, fileCliContext.getPmVirtualHost());
    if (jsonObject==null)
      return 1;
    outputFormatter.print(format, jsonObject);
    return 0;
  }

  public int pmDeleteAndPrint(String path,
                              OutputFormat format) throws IOException {
    var jsonObject = pmDelete(path);
    if (jsonObject==null)
      return 1;
    outputFormatter.print(format, jsonObject);
    return 0;
  }

  public JsonObject pmDelete(String path) throws IOException {
    FileCliConfig.FileCliContext fileCliContext = configFileManager.current();
    String pmUrl = fileCliContext.getPmUrl();
    return request(HttpMethod.DELETE, pmUrl + path, fileCliContext.getPmVirtualHost());
  }
}
