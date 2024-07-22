package org.hpcclab.oaas.invoker.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.controller.fn.*;
import org.hpcclab.oaas.invocation.controller.fn.logical.*;
import org.hpcclab.oaas.invocation.dataflow.DataflowOrchestrator;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.invoker.service.HashAwareInvocationHandler;
import org.hpcclab.oaas.repository.ObjectRepoManager;
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
  NewFnController newFunctionController(IdGenerator idGenerator,
                                        ObjectMapper mapper,
                                        ContentUrlGenerator contentUrlGenerator) {
    return new NewFnController(idGenerator, mapper, contentUrlGenerator);
  }

  @Produces
  CopyFnController copyFunctionController(IdGenerator idGenerator,
                                          ObjectMapper mapper) {
    return new CopyFnController(idGenerator, mapper);
  }

  @Produces
  UpdateFnController updateFunctionController(IdGenerator idGenerator,
                                              ObjectMapper mapper) {
    return new UpdateFnController(idGenerator, mapper);
  }

  @Produces
  GetFnController getFnController(IdGenerator idGenerator,
                                  ObjectMapper mapper) {
    return new GetFnController(idGenerator, mapper);
  }

  @Produces
  DeleteFnController deleteFnController(IdGenerator idGenerator,
                                        ObjectMapper mapper) {
    return new DeleteFnController(idGenerator, mapper);
  }

  @Produces
  ProjectFnController projectFnController(IdGenerator idGenerator,
                                          ObjectMapper mapper) {
    return new ProjectFnController(idGenerator, mapper);
  }

  @Produces
  FileFnController fileFnController(IdGenerator idGenerator,
                                    ObjectMapper mapper,
                                    ContentUrlGenerator generator,
                                    ObjectMapper objectMapper) {
    return new FileFnController(idGenerator, mapper, generator, objectMapper);
  }


  @Produces
  NativeQueryFnController nativeQueryFnController(IdGenerator idGenerator,
                                                  ObjectMapper mapper,
                                                  ObjectRepoManager repoManager) {
    return new NativeQueryFnController(idGenerator, mapper, repoManager);
  }

  @Produces
  CdiFunctionControllerFactory functionControllerFactory(Instance<TaskFunctionController> taskFunctionControllerInstance,
                                                         Instance<MacroFunctionController> macroFunctionControllerInstance,
                                                         Instance<ChainFunctionController> chainFunctionControllerInstance,
                                                         Instance<BuiltinFunctionController> logicalFunctionControllers) {
    return new CdiFunctionControllerFactory(taskFunctionControllerInstance,
      macroFunctionControllerInstance,
      chainFunctionControllerInstance,
      logicalFunctionControllers);
  }
}
