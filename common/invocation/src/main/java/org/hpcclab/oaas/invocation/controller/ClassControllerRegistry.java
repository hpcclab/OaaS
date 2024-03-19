package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

public interface ClassControllerRegistry {
  Uni<ClassController> registerOrUpdate(ProtoOClass cls);

  Uni<ClassController> registerOrUpdate(OClass cls);

  ClassController getClassController(String clsKey);

  void updateFunction(OFunction function);

  String printStructure();
}
