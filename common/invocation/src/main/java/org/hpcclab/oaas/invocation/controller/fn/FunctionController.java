package org.hpcclab.oaas.invocation.controller.fn;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.controller.InvocationCtx;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;

/**
 * @author Pawissanutt
 */
public interface FunctionController {
  Uni<InvocationCtx> invoke(InvocationCtx context);
  void bind(FunctionBinding functionBinding,
            OFunction function,
            OClass cls,
            OClass outputCls);
  OFunction getFunction();
  FunctionBinding getFunctionBinding();
}
