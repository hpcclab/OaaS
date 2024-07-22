package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.invocation.state.StateManager;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class RepoClassControllerBuilder extends AbsClassControllerBuilder {
  final FunctionRepository fnRepo;
  final ClassRepository clsRepo;

  public RepoClassControllerBuilder(FunctionControllerFactory functionControllerFactory,
                                    StateManager stateManager,
                                    IdGenerator idGenerator,
                                    InvocationQueueProducer invocationQueueProducer,
                                    MetricFactory metricFactory,
                                    FunctionRepository fnRepo,
                                    ClassRepository clsRepo) {
    super(functionControllerFactory, stateManager, idGenerator, invocationQueueProducer, metricFactory);
    this.fnRepo = fnRepo;
    this.clsRepo = clsRepo;
  }

  @Override
  protected Uni<Map<String, OClass>> listCls(Set<String> keys) {
    return clsRepo.async().listAsync(keys);
  }

  @Override
  protected Uni<Map<String, OFunction>> listFn(Set<String> keys) {
    return fnRepo.async().listAsync(keys);
  }
}
