package org.hpcclab.oaas.orbit.exception;

import org.hpcclab.oaas.model.exception.StdOaasException;

public class OrbitUpdateException extends StdOaasException {
  public OrbitUpdateException() {
    super(500);
  }
  public OrbitUpdateException(String msg) {
    super(msg, 500);
  }
  public OrbitUpdateException(Throwable e) {
    super(null, e);
  }
}
