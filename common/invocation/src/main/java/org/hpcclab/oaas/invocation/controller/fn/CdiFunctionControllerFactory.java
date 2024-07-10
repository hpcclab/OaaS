package org.hpcclab.oaas.invocation.controller.fn;

import jakarta.enterprise.inject.Instance;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OFunction;

/**
 * @author Pawissanutt
 */
public class CdiFunctionControllerFactory implements FunctionControllerFactory {
  final Instance<TaskFunctionController> taskFunctionControllerInstance;
  final  Instance<MacroFunctionController> macroFunctionControllerInstance;
  final  Instance<ChainFunctionController> chainFunctionControllerInstance;
  final  Instance<BuiltinFunctionController> logicalFunctionControllers;

  public CdiFunctionControllerFactory(Instance<TaskFunctionController> taskFunctionControllerInstance,
                                      Instance<MacroFunctionController> macroFunctionControllerInstance,
                                      Instance<ChainFunctionController> chainFunctionControllerInstance,
                                      Instance<BuiltinFunctionController> logicalFunctionControllers) {
    this.taskFunctionControllerInstance = taskFunctionControllerInstance;
    this.macroFunctionControllerInstance = macroFunctionControllerInstance;
    this.chainFunctionControllerInstance = chainFunctionControllerInstance;
    this.logicalFunctionControllers = logicalFunctionControllers;
  }

  public FunctionController create(OFunction function) {
    if (function.getType() == FunctionType.BUILTIN) {
        return logicalFunctionControllers
        .stream()
        .filter(fc -> fc.getFnKey().equals(function.getKey()))
        .findFirst()
        .orElseThrow();
    } else if (function.getType() == FunctionType.MACRO) {
      return macroFunctionControllerInstance
        .get();
    } else if (function.getType() == FunctionType.CHAIN) {
      return chainFunctionControllerInstance
        .get();
    } else {
      return taskFunctionControllerInstance
        .get();
    }
  }
}
