package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.function.OFunction;

/**
 * @author Pawissanutt
 */
public interface OffLoaderFactory {
  OffLoader create(OFunction function);
}
