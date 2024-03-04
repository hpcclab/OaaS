package org.hpcclab.oaas.pm.service;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

@ApplicationScoped
public class PackagePublisher {
  private static final Logger logger = LoggerFactory.getLogger( PackagePublisher.class );
  @Channel("fnProvisions")
  MutinyEmitter<Record<String, OFunction>> fnProvisionEmitter;
  @Channel("clsProvisions")
  MutinyEmitter<Record<String, OClass>> clsProvisionEmitter;


  public Uni<Void> submitNewFunction(OFunction function) {
    return fnProvisionEmitter
      .send(Record.of(function.getKey(), function));
  }


  public Uni<Void> submitNewCls(OClass cls) {
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


  public Uni<Void> submitNewFunction(Stream<OFunction> functions) {
    return Multi.createFrom().items(functions)
      .onItem()
      .transformToUniAndConcatenate(this::submitNewFunction)
      .collect().last();
  }

  public Uni<Void> submitNewCls(Stream<OClass> classStream) {
    return Multi.createFrom().items(classStream)
      .onItem()
      .transformToUniAndConcatenate(this::submitNewCls)
      .collect().last();
  }

  public Uni<Void> submitNewPkg(OPackage packageContainer) {
    logger.debug("publish pkg {}", packageContainer.getName());
    return submitNewFunction(packageContainer.getFunctions().stream())
      .flatMap(__ -> submitNewCls(packageContainer.getClasses().stream()));
  }
}
