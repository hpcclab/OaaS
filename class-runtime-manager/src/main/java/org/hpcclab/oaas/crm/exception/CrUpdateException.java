package org.hpcclab.oaas.crm.exception;

import org.hpcclab.oaas.model.exception.StdOaasException;

public class CrUpdateException extends StdOaasException {
  public CrUpdateException() {
    super(500);
  }
  public CrUpdateException(String msg) {
    super(msg, 500);
  }
  public CrUpdateException(Throwable e) {
    super(null, e);
  }
}
