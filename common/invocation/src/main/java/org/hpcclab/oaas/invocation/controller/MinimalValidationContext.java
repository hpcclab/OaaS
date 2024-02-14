package org.hpcclab.oaas.invocation.controller;

import lombok.Builder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;

/**
 * @author Pawissanutt
 */
@Builder()
public record MinimalValidationContext(
  InvocationRequest request,
  OClass cls,
  OFunction func,
  FunctionBinding fnBind
) {
}
