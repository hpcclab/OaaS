package org.hpcclab.oaas.exception;

public class ObjectValidationException extends NoStackException{
  public ObjectValidationException(String message) {
    super(message);
    setCode(400);
  }
}
