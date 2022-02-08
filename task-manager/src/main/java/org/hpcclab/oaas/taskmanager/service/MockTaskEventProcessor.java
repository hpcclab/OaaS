package org.hpcclab.oaas.taskmanager.service;

import io.vertx.mutiny.core.Vertx;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.taskmanager.TaskCompletionConsumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
@Alternative
public class MockTaskEventProcessor extends TaskEventProcessor{
  @Inject
  TaskCompletionConsumer taskCompletionConsumer;

  @Override
  public void submitTask(String id) {
//    super.submitTask(id);
    var uni  =taskCompletionConsumer.handle(List.of(
      new TaskCompletion().setId(id).setStatus(TaskStatus.SUCCEEDED)
    ));
    Vertx.currentContext().executeBlockingAndForget(uni);
  }
}
