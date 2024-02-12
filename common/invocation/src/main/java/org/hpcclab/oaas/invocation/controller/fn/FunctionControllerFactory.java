package org.hpcclab.oaas.invocation.controller.fn;

import org.hpcclab.oaas.model.function.OFunction;

/**
 * @author Pawissanutt
 */
public interface FunctionControllerFactory {
  FunctionController create(OFunction function) ;
}
