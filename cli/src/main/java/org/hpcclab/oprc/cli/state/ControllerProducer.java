package org.hpcclab.oprc.cli.state;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.controller.fn.*;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class ControllerProducer {
  @Produces
  @ApplicationScoped
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
