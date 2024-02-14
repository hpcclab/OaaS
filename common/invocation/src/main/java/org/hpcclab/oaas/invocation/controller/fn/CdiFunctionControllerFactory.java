package org.hpcclab.oaas.invocation.controller.fn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OFunction;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class CdiFunctionControllerFactory implements FunctionControllerFactory {
  Instance<TaskFunctionController> taskFunctionControllerInstance;
  Instance<MacroFunctionController> macroFunctionControllerInstance;
  Instance<LogicalFunctionController> logicalFunctionControllers;

  @Inject
  public CdiFunctionControllerFactory(Instance<TaskFunctionController> taskFunctionControllerInstance,
                                      Instance<MacroFunctionController> macroFunctionControllerInstance,
                                      Instance<LogicalFunctionController> logicalFunctionControllers) {
    this.taskFunctionControllerInstance = taskFunctionControllerInstance;
    this.macroFunctionControllerInstance = macroFunctionControllerInstance;
    this.logicalFunctionControllers = logicalFunctionControllers;
  }

  public FunctionController create(OFunction function) {
    if (function.getType() == FunctionType.LOGICAL) {
        return logicalFunctionControllers
        .stream()
        .filter(fc -> fc.getFnKey().equals(function.getKey()))
        .findFirst()
        .orElseThrow();
    } else if (function.getType() == FunctionType.MACRO) {
      return macroFunctionControllerInstance
        .get();
    } else {
      return taskFunctionControllerInstance
        .get();
    }
  }
}
