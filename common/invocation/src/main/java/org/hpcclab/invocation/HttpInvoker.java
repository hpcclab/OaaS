package org.hpcclab.invocation;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class HttpInvoker implements Invoker{
  private static final Logger LOGGER = LoggerFactory.getLogger( HttpInvoker.class );
  @Inject
  WebClient webClient;
  @Inject
  FunctionRepository funcRepo;

  @Override
  public Uni<TaskCompletion> invoke(OaasTask task) {
    var funcName = task.getOutput().getOrigin().getFuncName();
    return funcRepo.getAsync(funcName)
      .flatMap(function -> {
        var url = function.getDeploymentStatus().getInvocationUrl();
        return webClient.post(url)
          .sendJson(task)
          .map(resp -> this.handleResp(task, resp))
          .onFailure()
          .recoverWithItem(e -> new TaskCompletion(
            task.getId(),
            false,
            "Fail to perform invocation: " + e.getMessage(),
            null
          ));
      });
  }

  TaskCompletion handleResp(OaasTask task, HttpResponse<Buffer> resp) {
    if (resp.statusCode() == 200)
      return TaskDecoder.tryDecode(task.getId(), resp.bodyAsBuffer().getDelegate());
    else
      return new TaskCompletion(
        task.getId(),
        false,
        "Fail to perform invocation: function return not 200 code (%s)"
          .formatted(resp.cookies()),
        null
      );
  }

}
