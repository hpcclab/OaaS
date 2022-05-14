package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasWorkflowStep;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ContextLoader {
  Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLangauge request);

  Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx, OaasWorkflowStep step);
  Uni<TaskContext> getTaskContextAsync(OaasObject output);
}
