package org.hpcclab.msc.object.exception;

public class ObjectValidationException extends NoStackException{
  public ObjectValidationException(String message) {
    super(message);
    setCode(400);
  }
}
