package org.hpcclab.oaas.model.exception;

public class ObjectValidationException extends NoStackException{
  public ObjectValidationException(String message) {
    super(message);
    setCode(400);
  }
}
