package org.hpcclab.oaas.invocation.controller;

import lombok.Builder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.invocation.InvocationRequest;

/**
 * @author Pawissanutt
 */
@Builder()
public record MinimalValidationContext(
  InvocationRequest request,
  OClass cls,
  OFunction func,
  FunctionBinding fb
) {
}
