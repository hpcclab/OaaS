package org.hpcclab.oaas.model.task;

import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.List;
import java.util.Map;

public interface TaskDetail {
  String getVId();
  OaasObject getMain();
  OaasObject getOutput();
  String getFuncKey();
  String getFbName();
  boolean isImmutable();
  List<OaasObject> getInputs();
  Map<String, String> getArgs();
  default InvocationNode getNode() {
    throw new UnsupportedOperationException();
  }

  default InvocationRequest toRequest() {
    return InvocationRequest.builder()
      .partKey(getMain() != null? getMain().getId() : null)
      .macro(false)
      .args(getArgs())
      .inputs(getInputs().stream().map(OaasObject::getId).toList())
      .targetCls(getMain().getCls())
      .target(getMain().getId())
      .fb(getFbName())
      .outId(getOutput() != null? getOutput().getId() : null)
      .immutable(isImmutable())
//      .function(getFuncKey())
      .queTs(System.currentTimeMillis())
      .loadOutput(getOutput() != null)
      .build();
  }
}
