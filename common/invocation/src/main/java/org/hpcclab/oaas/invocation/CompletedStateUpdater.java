package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompletedStateUpdater {
  private static final Logger logger = LoggerFactory.getLogger( CompletedStateUpdater.class );

  CompletionValidator validator;

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
        logger.debug("updated main {}", main);
      }
      main.setLastOffset(context.getMqOffset());
      if (completion.isSuccess()) {
        main.setLastInv(completion.getId());
      }
    }

    if (out!=null) {
      if (completion.getOutput()!=null)
        completion.getOutput().update(out, completion
          .getId());
      if (completion.isSuccess()) {
        out.setLastInv(completion.getId());
      }
    }

    context.setRespBody(completion.getBody());
  }
}
