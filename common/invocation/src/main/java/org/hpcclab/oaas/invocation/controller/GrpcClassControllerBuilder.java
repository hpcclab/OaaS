package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.FunctionService;
import org.hpcclab.oaas.proto.MultiKeyQuery;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class GrpcClassControllerBuilder extends ClassControllerBuilder {
  final ClassService classService;
  final FunctionService functionService;

  public GrpcClassControllerBuilder(FunctionControllerFactory functionControllerFactory,
                                       StateManager stateManager,
                                       IdGenerator idGenerator,
                                       InvocationQueueProducer invocationQueueProducer,
                                       MetricFactory metricFactory,
                                       ClassService classService,
                                       FunctionService functionService) {
    super(functionControllerFactory, stateManager, idGenerator, invocationQueueProducer, metricFactory);
    this.classService = classService;
    this.functionService = functionService;
  }


  @Override
  protected Uni<Map<String, OClass>> listCls(Set<String> keys) {
    return classService.select(MultiKeyQuery.newBuilder().addAllKey(keys).build())
      .map(protoMapper::fromProto)
      .collect()
      .asMap(OClass::getKey);
  }

  @Override
  protected Uni<Map<String, OFunction>> listFn(Set<String> keys) {
    return functionService.select(MultiKeyQuery.newBuilder().addAllKey(keys).build())
      .map(protoMapper::fromProto)
      .collect().asMap(OFunction::getKey)
      ;
  }
}
