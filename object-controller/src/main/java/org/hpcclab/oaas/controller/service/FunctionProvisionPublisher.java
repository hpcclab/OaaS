package org.hpcclab.oaas.controller.service;


import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.stream.Stream;

@ApplicationScoped
public class FunctionProvisionPublisher {
  @Channel("provisions")
  MutinyEmitter<Record<String, OaasFunction>> provisionEmitter;
//  @Inject
//  CtxMapper mapper;

  public Uni<Void> submitNewFunction(OaasFunction function) {
    var provision = function.getProvision();
    if (provision==null || provision.getKnative()==null) {
      return Uni.createFrom().nullItem();
    }
    return provisionEmitter
      .send(Record.of(function.getKey(), function));
  }

  public Uni<Void> submitDelete(String funcName) {
    return provisionEmitter
      .send(Record.of(funcName, null));
  }


  public Uni<Void> submitNewFunction(Stream<OaasFunction> functions) {
    return Multi.createFrom().items(functions)
      .onItem()
      .transformToUniAndConcatenate(this::submitNewFunction)
      .collect().last();
  }
}
