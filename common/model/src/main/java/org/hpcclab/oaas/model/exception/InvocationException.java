package org.hpcclab.oaas.model.exception;

public class InvocationException extends StdOaasException {
  public InvocationException(String message, Throwable cause) {
    super(message, cause, true, 500);
  }

  public InvocationException(String message) {
    super(message, null, true, 500);
  }

  public InvocationException(String message, int code) {
    super(message, code);
  }

  public InvocationException(String message, Throwable cause, int code) {
    super(message, cause, true, code);
  }

  public static InvocationException detectConcurrent(Throwable e) {
    return new InvocationException("Detect concurrent update in the same object", e, 409);
  }
}
