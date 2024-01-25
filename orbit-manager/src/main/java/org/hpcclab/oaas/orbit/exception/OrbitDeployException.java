package org.hpcclab.oaas.orbit.exception;

import org.hpcclab.oaas.model.exception.StdOaasException;

public class OrbitDeployException extends StdOaasException {
  public OrbitDeployException() {
    super(500);
  }
  public OrbitDeployException(String msg) {
    super(msg, 500);
  }
  public OrbitDeployException(Throwable e) {
    super(null, e);
  }
}
