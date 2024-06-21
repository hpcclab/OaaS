package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public interface ClassControllerBuilder {
  Uni<ClassController> build(ProtoOClass cls);
  Uni<ClassController> build(OClass cls);
  UnaryOperator<FunctionController> createUpdator(OFunction fn);
}
