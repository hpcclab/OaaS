package org.hpcclab.oaas.invocation;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.MultiMap;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class HttpInvoker implements SyncInvoker {
  private static final Logger LOGGER = LoggerFactory.getLogger( HttpInvoker.class );
  WebClient webClient;
  TaskFactory taskFactory;
  HttpInvokerConfig config;

  @Inject
  public HttpInvoker(WebClient webClient,
                     TaskFactory taskFactory,
                     HttpInvokerConfig config) {
    this.webClient = webClient;
    this.taskFactory = taskFactory;
    this.config = config;
  }

  @Override
  public Uni<TaskCompletion> invoke(TaskContext taskContext) {
    var task = taskFactory.genTask(taskContext);
    return invoke(task);
  }

  @Override
  public Uni<TaskCompletion> invoke(InvokingDetail<?> invokingDetail) {
    var content = invokingDetail.getContent();
    Buffer contentBuffer;
    if (content instanceof Buffer buffer) {
      contentBuffer = buffer;
    } else if (content instanceof io.vertx.core.buffer.Buffer buffer){
      contentBuffer = Buffer.newInstance(buffer);
    }else {
      contentBuffer = Buffer.newInstance(Json.encodeToBuffer(content));
    }
    return webClient.postAbs(invokingDetail.getFuncUrl())
          .putHeaders(createHeader(invokingDetail))
          .sendBuffer(contentBuffer)
          .map(resp -> this.handleResp(invokingDetail, resp))
          .onFailure()
          .recoverWithItem(e -> new TaskCompletion(
            invokingDetail.getId(),
            false,
            "Fail to perform invocation: " + e.getMessage(),
            null)
          );
  }

  protected MultiMap createHeader(InvokingDetail<?> detail) {
    return MultiMap.caseInsensitiveMultiMap()
      .add("ce-type", config.getCeType())
      .add("ce-function", config.getAppName())
      .add("ce-id", detail.getId())
      .add("ce-source", config.getAppName())
      .add("content-type", "application/json");
  }

  TaskCompletion handleResp(InvokingDetail<?> detail, HttpResponse<Buffer> resp) {
    if (resp.statusCode() == 200)
      return TaskDecoder.tryDecode(detail.getId(), resp.bodyAsBuffer().getDelegate());
    else
      return new TaskCompletion(
        detail.getId(),
        false,
        "Fail to perform invocation: function return not 200 code (%s)"
          .formatted(resp.cookies()),
        null
      );
  }

}
