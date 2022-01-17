package org.hpcclab.oaas.model.exception;

public class FunctionValidationException extends NoStackException{
  public FunctionValidationException(String message) {
    super(message, 400);
  }

  public FunctionValidationException(String message, Throwable cause) {
    super(message, cause, 400);
  }
}
