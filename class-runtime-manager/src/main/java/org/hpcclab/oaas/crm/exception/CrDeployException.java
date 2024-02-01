package org.hpcclab.oaas.crm.exception;

import org.hpcclab.oaas.model.exception.StdOaasException;

public class CrDeployException extends StdOaasException {
  public CrDeployException() {
    super(500);
  }
  public CrDeployException(String msg) {
    super(msg, 500);
  }
  public CrDeployException(Throwable e) {
    super(null, e);
  }
}
