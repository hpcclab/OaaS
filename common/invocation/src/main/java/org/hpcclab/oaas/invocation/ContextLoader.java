package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OObject;

public interface ContextLoader {
  Uni<InvocationContext> loadCtxAsync(InvocationRequest request);

  InvocationContext loadClsAndFunc(InvocationContext ctx, String fbName);
  Uni<OObject> resolveObj(InvocationContext ctx, String refName);
}
