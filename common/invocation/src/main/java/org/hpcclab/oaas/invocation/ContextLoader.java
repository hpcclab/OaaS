package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ContextLoader {
  Uni<InvocationContext> loadCtxAsync(InvocationRequest request);

  InvocationContext loadClsAndFunc(InvocationContext ctx, String fbName);
//  Uni<TaskContext> getTaskContextAsync(OaasObject output);

  Uni<OaasObject> resolveObj(InvocationContext ctx, String refName);
}
