package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface ClassControllerRegistry {
  ClassController register(ClassController classController);

  ClassController getClassController(String clsKey);

  void updateFunction(OFunction function, UnaryOperator<FunctionController> updater);

  String printStructure();
}
