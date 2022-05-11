package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class InvocationGraphExecutor {
  TaskSubmitter submitter;
  GraphStateManager gsm;

  public InvocationGraphExecutor(TaskSubmitter submitter, GraphStateManager gsm) {
    this.submitter = submitter;
    this.gsm = gsm;
  }

  boolean checkIfReadyToSubmit(OaasObject output,
                               OaasObject main,
                               List<OaasObject> inputs,
                               List<OaasObject> idleObjs) {
    boolean completed = true;
    boolean fail = false;
    for (var o : inputs) {
      var ts = o.getStatus().getTaskStatus();
      completed &= o.isReadyToUsed();
      fail |= ts==TaskStatus.FAILED;
      if (!ts.isSubmitted() && !ts.isFailed())
        idleObjs.add(o);
    }

    var ts = main.getStatus().getTaskStatus();
    completed &= main.isReadyToUsed();
    fail |= ts==TaskStatus.FAILED;
    if (!ts.isSubmitted() && !ts.isFailed())
      idleObjs.add(main);

    if (completed)
      return true;
    if (fail)
      output.getStatus().setTaskStatus(TaskStatus.DEPENDENCY_FAILED);
    return false;
  }

  public Uni<Void> exec(FunctionExecContext ctx) {
    List<OaasObject> idleObjs = new ArrayList<>();
    boolean ready = checkIfReadyToSubmit(ctx.getOutput(),
      ctx.getMain(), ctx.getInputs(), idleObjs);
    if (ready) {
      return submitter.submit(ctx);
    }

    return null;
  }

  public Uni<Void> exec(OaasObject obj) {

    return null;
  }

  public Uni<Void> loadAndSubmit(FunctionExecContext ctx, OaasObject object) {
    return null;
  };
}
