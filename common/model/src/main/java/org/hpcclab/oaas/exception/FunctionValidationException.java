package org.hpcclab.oaas.exception;

public class FunctionValidationException extends NoStackException{
  public FunctionValidationException(String message) {
    super(message);
    setCode(400);
  }

  public FunctionValidationException(String message, Throwable cause) {
    super(message, cause);
    setCode(400);
  }
}
