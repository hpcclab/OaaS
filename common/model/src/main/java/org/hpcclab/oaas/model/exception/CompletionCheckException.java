package org.hpcclab.oaas.model.exception;

import org.hpcclab.oaas.model.object.OaasObject;

public class CompletionCheckException extends StdOaasException {
  public CompletionCheckException(String message, Throwable cause) {
    super(message, cause, true, 400);
  }

  public CompletionCheckException(String message) {
    super(message, null, true, 400);
  }
}
