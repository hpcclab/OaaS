package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.FunctionService;
import org.hpcclab.oaas.proto.MultiKeyQuery;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */

public class ClassControllerRegistry extends AbsClassControllerRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ClassControllerRegistry.class);
  final ClassService classService;
  final FunctionService functionService;

  public ClassControllerRegistry() {
    this.classService = null;
    this.functionService = null;
  }

  public ClassControllerRegistry(ClassService classService,
                                 FunctionService functionService,
                                 FunctionControllerFactory functionControllerFactory,
                                 StateManager stateManager,
                                 IdGenerator idGenerator,
                                 InvocationQueueProducer invocationQueueProducer,
                                 MetricFactory metricFactory) {
    super(
      functionControllerFactory,
      stateManager,
      idGenerator,
      invocationQueueProducer,
      metricFactory
    );
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
