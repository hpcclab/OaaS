package org.hpcclab.oaas.invoker.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.controller.fn.CdiFunctionControllerFactory;
import org.hpcclab.oaas.invocation.controller.fn.LogicalFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.MacroFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.TaskFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.CopyFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.FanInFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.logical.UpdateFunctionController;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class FunctionControllerProducer {
  @Produces
  TaskFunctionController taskFunctionController(IdGenerator idGenerator,
                                                ObjectMapper mapper,
                                                OffLoader offLoader,
                                                ContentUrlGenerator contentUrlGenerator) {
    return new TaskFunctionController(idGenerator,
      mapper, offLoader, contentUrlGenerator);
  }

  @Produces
  MacroFunctionController macroFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper) {
    return new MacroFunctionController(idGenerator, mapper);
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
  FanInFunctionController fanInFunctionController(IdGenerator idGenerator,
                                                  ObjectMapper mapper) {
    return new FanInFunctionController(idGenerator, mapper);
  }


  @Produces
  CdiFunctionControllerFactory functionControllerFactory(Instance<TaskFunctionController> taskFunctionControllerInstance,
                                                         Instance<MacroFunctionController> macroFunctionControllerInstance,
                                                         Instance<LogicalFunctionController> logicalFunctionControllers) {
    return new CdiFunctionControllerFactory(taskFunctionControllerInstance,
      macroFunctionControllerInstance,
      logicalFunctionControllers);
  }
}
