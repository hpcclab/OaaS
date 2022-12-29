package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ContextLoader {
  Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLanguage request);
  Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx, DataflowStep step);
  Uni<TaskContext> getTaskContextAsync(OaasObject output);
  Uni<TaskContext> getTaskContextAsync(String outputId);
}
