package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public interface ClassController {
  OClass getCls();
  Map<String, FunctionController> getFunctionControllers();
  FunctionController getFunctionController(String fb);
  MinimalValidationContext validate(ObjectAccessLanguage oal);
  Uni<Void> enqueue(InvocationRequest req);
  Uni<InvocationCtx> invoke(InvocationCtx context);
  void updateFunctionController(String fnKey, UnaryOperator<FunctionController> updater);
}
