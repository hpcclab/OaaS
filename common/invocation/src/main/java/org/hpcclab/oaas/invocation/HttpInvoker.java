package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
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
  @Inject
  WebClient webClient;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  TaskFactory taskFactory;

  @Override
  public Uni<TaskCompletion> invoke(OaasTask task) {
    var funcName = task.getOutput().getOrigin().getFuncName();
    var taskFunc = task.getFunction();
    Uni<OaasFunction> functionUni = taskFunc == null?
      funcRepo.getAsync(funcName):
      Uni.createFrom().item(taskFunc);
    return functionUni
      .flatMap(function -> {
        var url = function.getDeploymentStatus().getInvocationUrl();
        return webClient.postAbs(url)
          .putHeaders(createHeader(task))
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

  @Override
  public Uni<TaskCompletion> invoke(TaskContext taskContext) {
    var task = taskFactory.genTask(taskContext);
    return invoke(task);
  }

  protected MultiMap createHeader(OaasTask task) {
    return MultiMap.caseInsensitiveMultiMap()
      .add("ce-type", "oaas.task")
      .add("ce-function", task.getFunction().getName())
      .add("ce-id", task.getId())
      .add("ce-source", "oaas/task-manager");
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
