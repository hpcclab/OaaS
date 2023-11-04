package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.task.TaskCompletion;

@ApplicationScoped
public class CompletedStateUpdater {

  CompletionValidator validator;

  @Inject
  public CompletedStateUpdater(CompletionValidator validator) {
    this.validator = validator;
  }

  public Uni<TaskCompletion> handleComplete(InvocationContext context, TaskCompletion completion) {
    return validator.validateCompletion(context, completion)
      .invoke(tc -> updateState(context, completion))
      .replaceWith(completion);
  }

  void updateState(InvocationContext context, TaskCompletion completion) {
    var main = context.getMain();
    var out = context.getOutput();
    var node = context.initNode();
    node.updateStatus(completion);

    if (main!=null) {
      if (completion.getMain()!=null) {
        completion.getMain().update(main, context.initNode().getKey());
        if (out==null && completion.isSuccess())
          main.getStatus().set(completion);
      }
      main.getStatus().setUpdatedOffset(context.getMqOffset());
    }

    if (out!=null) {
      out.getStatus().set(completion);
      if (completion.getOutput()!=null)
        completion.getOutput().update(out, completion
          .getId().iid());
    }

    context.setRespBody(completion.getBody());
  }
}
