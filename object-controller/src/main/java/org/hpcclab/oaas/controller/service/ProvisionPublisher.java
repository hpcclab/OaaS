package org.hpcclab.oaas.controller.service;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.stream.Stream;

@ApplicationScoped
public class ProvisionPublisher {
  @Channel("fnProvisions")
  MutinyEmitter<Record<String, OaasFunction>> fnProvisionEmitter;
  @Channel("clsProvisions")
  MutinyEmitter<Record<String, OaasClass>> clsProvisionEmitter;


  public Uni<Void> submitNewFunction(OaasFunction function) {
    return fnProvisionEmitter
      .send(Record.of(function.getKey(), function));
  }


  public Uni<Void> submitNewCls(OaasClass cls) {
    return clsProvisionEmitter
      .send(Record.of(cls.getKey(), cls));
  }

  public Uni<Void> submitDeleteFn(String funcName) {
    return fnProvisionEmitter
      .send(Record.of(funcName, null));
  }


  public Uni<Void> submitDeleteCls(String clsName) {
    return clsProvisionEmitter
      .send(Record.of(clsName, null));
  }


  public Uni<Void> submitNewFunction(Stream<OaasFunction> functions) {
    return Multi.createFrom().items(functions)
      .onItem()
      .transformToUniAndConcatenate(this::submitNewFunction)
      .collect().last();
  }

  public Uni<Void> submitNewCls(Stream<OaasClass> classStream) {
    return Multi.createFrom().items(classStream)
      .onItem()
      .transformToUniAndConcatenate(this::submitNewCls)
      .collect().last();
  }

  public Uni<Void> submitNewPkg(OaasPackageContainer packageContainer) {
    return submitNewFunction(packageContainer.getFunctions().stream())
      .flatMap(__ -> submitNewCls(packageContainer.getClasses().stream()));
  }
}
