package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CompletedStateUpdater {

  CompletionValidator validator;

  @Inject
  public CompletedStateUpdater(CompletionValidator validator) {
    this.validator = validator;
  }

  public Uni<TaskCompletion> handleComplete(TaskDetail task, TaskCompletion completion) {
    return validator.validateCompletion(task, completion)
      .invoke(tc -> updateState(task, completion))
      .replaceWith(completion);
  }

  void updateState(TaskDetail task, TaskCompletion completion) {
    var main = task.getMain();
    var out = task.getOutput();

    if (main!=null) {
      if (completion.getMain()!=null) {
        completion.getMain().update(main, task.getVId());
        if (out == null && completion.isSuccess())
          main.getStatus().set(completion);
      }
      if (task instanceof InvocationContext iac && iac.getMqOffset() >= 0)
        main.getStatus().setUpdatedOffset(iac.getMqOffset());
    }

    if (out!=null) {
      out.getStatus().set(completion);
      if (completion.getOutput() != null)
        completion.getOutput().update(out, completion
          .getId().getVid());
      if (task instanceof InvocationContext iac && iac.getMqOffset() >= 0)
        out.getStatus().setUpdatedOffset(iac.getMqOffset());
    }
  }
}
