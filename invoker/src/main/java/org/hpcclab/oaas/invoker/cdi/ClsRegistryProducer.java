package org.hpcclab.oaas.invoker.cdi;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.AbsClassControllerBuilder;
import org.hpcclab.oaas.invocation.controller.BaseClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.GrpcClassControllerBuilder;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.invocation.state.StateManager;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.FunctionService;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class ClsRegistryProducer {

  @Produces
  @ApplicationScoped
  ClassControllerRegistry registry() {
    return new BaseClassControllerRegistry();
  }

  @Produces
  @ApplicationScoped
  AbsClassControllerBuilder builder(@GrpcClient("package-manager") ClassService classService,
                                    @GrpcClient("package-manager") FunctionService functionService,
                                    FunctionControllerFactory functionControllerFactory,
                                    StateManager stateManager,
                                    IdGenerator idGenerator,
                                    InvocationQueueProducer invocationQueueProducer,
                                    MetricFactory metricFactory) {
    return new GrpcClassControllerBuilder(
      functionControllerFactory,
      stateManager,
      idGenerator,
      invocationQueueProducer,
      metricFactory,
      classService,
      functionService
    );
  }
}
