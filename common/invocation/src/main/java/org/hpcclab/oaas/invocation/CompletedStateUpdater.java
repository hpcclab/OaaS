package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
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

    if (main!=null) {
      if (completion.getMain()!=null) {
        completion.getMain().update(main, task.getVId());
      }
      if (task instanceof InvApplyingContext iac && iac.getMqOffset() >= 0)
        main.getStatus().setUpdatedOffset(iac.getMqOffset());
    }

    var out = task.getOutput();
    if (out!=null) {
      out.updateStatus(completion);
      if (task instanceof InvApplyingContext iac && iac.getMqOffset() >= 0)
        out.getStatus().setUpdatedOffset(iac.getMqOffset());
    }
  }
}
