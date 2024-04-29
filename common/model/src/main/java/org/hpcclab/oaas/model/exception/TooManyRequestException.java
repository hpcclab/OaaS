package org.hpcclab.oaas.model.exception;

/**
 * @author Pawissanutt
 */
public class TooManyRequestException extends StdOaasException {
  public TooManyRequestException() {
    super("too many requests", 429);
  }
}
