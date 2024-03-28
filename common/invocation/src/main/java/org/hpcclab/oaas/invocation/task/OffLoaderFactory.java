package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.function.OFunction;

import java.util.Map;

/**
 * @author Pawissanutt
 */
public interface OffLoaderFactory {
  OffLoader create(OFunction function);
  OffLoader create(String type, Map<String,String> config);
}
