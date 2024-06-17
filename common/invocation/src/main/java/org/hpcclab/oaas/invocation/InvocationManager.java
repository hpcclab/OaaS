package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerBuilder;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public class InvocationManager {
  private final ClassControllerRegistry registry;
  private final ClassControllerBuilder classControllerBuilder;
  private final InvocationReqHandler reqHandler;

  public InvocationManager(ClassControllerRegistry registry, ClassControllerBuilder classControllerBuilder, InvocationReqHandler reqHandler) {
    this.registry = registry;
    this.classControllerBuilder = classControllerBuilder;
    this.reqHandler = reqHandler;
  }

  Uni<Void> update(OClass cls) {
    return
      classControllerBuilder.build(cls)
        .invoke(registry::register)
        .replaceWithVoid();
  }

  Uni<Void> update(ProtoOClass cls) {
    return
      classControllerBuilder.build(cls)
        .invoke(registry::register)
        .replaceWithVoid();
  }

  Uni<Void> update(OFunction fn) {
    UnaryOperator<FunctionController> updator = classControllerBuilder.createUpdator(fn);
    registry.updateFunction(fn, updator);
    return Uni.createFrom().nullItem();
  }

  public InvocationReqHandler getReqHandler() {
    return reqHandler;
  }

  public ClassControllerRegistry getRegistry() {
    return registry;
  }
}
