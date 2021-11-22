package org.hpcclab.oaas.model.exception;

public class OaasValidationException extends NoStackException{
  public OaasValidationException(String message) {
    super(message);
    setCode(400);
  }
}
