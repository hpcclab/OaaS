package org.hpcclab.oaas.invocation.task;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.invocation.config.HttpOffLoaderConfig;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOffLoader implements OffLoader {
  private static final Logger logger = LoggerFactory.getLogger(HttpOffLoader.class);
  WebClient webClient;
  HttpOffLoaderConfig config;

  public HttpOffLoader(WebClient webClient,
                       HttpOffLoaderConfig config) {
    this.webClient = webClient;
    this.config = config;
  }

  @Override
  public Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail) {
    if (logger.isDebugEnabled())
      logger.debug("invoke {}", invokingDetail.getId());
    if (invokingDetail.getFuncUrl()==null) {
      throw StdOaasException.format("Function is not ready");
    }
    var content = invokingDetail.getContent();
    Buffer contentBuffer;
    if (content instanceof Buffer buffer) {
      contentBuffer = buffer;
    } else if (content instanceof io.vertx.core.buffer.Buffer buffer) {
      contentBuffer = Buffer.newInstance(buffer);
    } else {
      contentBuffer = Buffer.newInstance(Json.encodeToBuffer(content));
    }
    return webClient.postAbs(invokingDetail.getFuncUrl())
      .putHeaders(createHeader(invokingDetail))
      .timeout(config.getTimout())
      .sendBuffer(contentBuffer)
      .map(resp -> this.handleResp(invokingDetail, resp))
      .onFailure()
      .transform(InvocationException::connectionErr)
      ;
  }

  protected MultiMap createHeader(InvokingDetail<?> detail) {
    if (config.isEnabledCeHeader()) {
      return MultiMap.caseInsensitiveMultiMap()
        .add("ce-type", config.getCeType())
        .add("ce-func", detail.getFuncName())
        .add("ce-id", detail.getId())
        .add("ce-source", config.getAppName())
        .add("ce-specversion", "1.0")
        .add("content-type", "application/json");
    } else {
      return MultiMap.caseInsensitiveMultiMap()
        .add("content-type", "application/json");
    }
  }

  TaskCompletion handleResp(InvokingDetail<?> detail, HttpResponse<Buffer> resp) {
    if (resp.statusCode()==200)
      return TaskDecoder.tryDecode(detail.getId(), resp.bodyAsBuffer().getDelegate())
        .setSmtTs(detail.getSmtTs());
    else
      return TaskCompletion.error(
        detail.getId(),
        "Fail to perform invocation: func return not 200 code (%s)"
          .formatted(resp.statusCode()),
        System.currentTimeMillis(),
        detail.smtTs
      );
  }

}
