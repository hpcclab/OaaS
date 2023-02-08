package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ContextLoader {
  Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLanguage request);
  Uni<FunctionExecContext> loadCtxAsync(InvocationRequest request);
//  Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx, DataflowStep step);

  FunctionExecContext loadClsAndFunc(FunctionExecContext ctx, String fbName);
  Uni<TaskContext> getTaskContextAsync(OaasObject output);
  Uni<TaskContext> getTaskContextAsync(String outputId);

  Uni<OaasObject> resolveObj(FunctionExecContext ctx, String refName);
}
