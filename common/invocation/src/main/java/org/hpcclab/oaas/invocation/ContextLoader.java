package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ContextLoader {
  Uni<InvApplyingContext> loadCtxAsync(ObjectAccessLanguage request);
  Uni<InvApplyingContext> loadCtxAsync(InvocationRequest request);
//  Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx, DataflowStep step);

  InvApplyingContext loadClsAndFunc(InvApplyingContext ctx, String fbName);
  Uni<TaskContext> getTaskContextAsync(OaasObject output);
  Uni<TaskContext> getTaskContextAsync(String outputId);

  Uni<OaasObject> resolveObj(InvApplyingContext ctx, String refName);
}
