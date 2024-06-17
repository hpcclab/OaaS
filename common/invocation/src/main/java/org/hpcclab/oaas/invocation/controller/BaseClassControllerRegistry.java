package org.hpcclab.oaas.invocation.controller;

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.function.OFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public class BaseClassControllerRegistry implements ClassControllerRegistry {
  private static final Logger logger = LoggerFactory.getLogger(BaseClassControllerRegistry.class);
  protected final Map<String, ClassController> classControllerMap = new ConcurrentHashMap<>();


  @Override
  public ClassController register(ClassController classController) {
    classControllerMap.put(classController.getCls().getKey(), classController);
    return classController;
  }

  @Override
  public void updateFunction(OFunction function, UnaryOperator<FunctionController> updater) {
    for (ClassController controller : classControllerMap.values()) {
      controller.updateFunctionController(function.getKey(), updater);
    }
  }

  public ClassController getClassController(String clsKey) {
    return classControllerMap.get(clsKey);
  }

  public String printStructure() {
    StringBuilder builder = new StringBuilder();
    for (ClassController classController : classControllerMap.values()) {
      builder.append("- ")
        .append(classController.getCls().getKey())
        .append(": [");
      for (var functionController : classController.getFunctionControllers().values()) {
        builder.append(functionController.getFunctionBinding().getName())
          .append(":")
          .append(functionController.getFunction().getKey())
          .append(",");
      }
      builder.append("]\n");
    }
    return builder.toString();
  }

}
