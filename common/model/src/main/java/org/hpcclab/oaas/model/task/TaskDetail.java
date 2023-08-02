package org.hpcclab.oaas.model.task;

import org.hpcclab.oaas.model.object.OaasObject;

import java.util.List;
import java.util.Map;

public interface TaskDetail {
  String getVid();
  OaasObject getMain();
  OaasObject getOutput();
  String getFuncKey();
  String getFbName();
  boolean isImmutable();
  List<OaasObject> getInputs();
  Map<String, String> getArgs();
}
