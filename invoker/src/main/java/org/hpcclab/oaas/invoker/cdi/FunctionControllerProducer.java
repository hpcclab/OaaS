package org.hpcclab.oaas.invoker.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.controller.fn.*;
import org.hpcclab.oaas.invocation.dataflow.DataflowOrchestrator;
import org.hpcclab.oaas.invocation.controller.fn.logical.CopyFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.UpdateFunctionController;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class FunctionControllerProducer {
  @Produces
  TaskFunctionController taskFunctionController(IdGenerator idGenerator,
                                                ObjectMapper mapper,
                                                OffLoaderFactory offLoaderFactory,
                                                ContentUrlGenerator contentUrlGenerator) {
    return new TaskFunctionController(idGenerator,
      mapper, offLoaderFactory, contentUrlGenerator);
  }

  @Produces
  MacroFunctionController macroFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper,
                                                  DataflowOrchestrator orchestrator) {
    return new MacroFunctionController(idGenerator, mapper, orchestrator);
  }

  @Produces
  ChainFunctionController chainFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper) {
    return new ChainFunctionController(idGenerator, mapper);
  }

  @Produces
  DataflowOrchestrator dataflowOrchestrator(HashAwareInvocationHandler invocationHandler,
                                            IdGenerator idGenerator) {
    return new DataflowOrchestrator(invocationHandler, idGenerator);
  }


  @Produces
  NewFunctionController newFunctionController(IdGenerator idGenerator,
                                              ObjectMapper mapper,
                                              DataUrlAllocator dataUrlAllocator) {
    return new NewFunctionController(idGenerator, mapper, dataUrlAllocator);
  }

  @Produces
  CopyFunctionController copyFunctionController(IdGenerator idGenerator,
                                                ObjectMapper mapper) {
    return new CopyFunctionController(idGenerator, mapper);
  }

  @Produces
  UpdateFunctionController updateFunctionController(IdGenerator idGenerator,
                                                    ObjectMapper mapper) {
    return new UpdateFunctionController(idGenerator, mapper);
  }

  @Produces
  CdiFunctionControllerFactory functionControllerFactory(Instance<TaskFunctionController> taskFunctionControllerInstance,
                                                         Instance<MacroFunctionController> macroFunctionControllerInstance,
                                                         Instance<ChainFunctionController> chainFunctionControllerInstance,
                                                         Instance<LogicalFunctionController> logicalFunctionControllers) {
    return new CdiFunctionControllerFactory(taskFunctionControllerInstance,
      macroFunctionControllerInstance,
      chainFunctionControllerInstance,
      logicalFunctionControllers);
  }
}
