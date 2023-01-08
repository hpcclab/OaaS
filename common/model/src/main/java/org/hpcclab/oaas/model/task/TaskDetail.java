package org.hpcclab.oaas.model.task;

import org.hpcclab.oaas.model.object.OaasObject;

public interface TaskDetail {
//  String getId();
  String getVId();
  OaasObject getMain();
  OaasObject getOutput();

  String getFuncName();
}
