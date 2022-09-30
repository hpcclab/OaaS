package org.hpcclab.oaas.model.exception;

import java.util.UUID;

public class NoStackException extends StdOaasException{

  public static final NoStackException INSTANCE = new NoStackException("INSTANCE",500);

  public NoStackException(int code) {
    super(null, null, false, code);
  }

  public NoStackException(String message) {
    super(message, null, false, 500);
  }


  public NoStackException(String message, int code) {
    super(message, null, false, code);
  }

  public NoStackException(String message, Throwable cause) {
    super(message, cause, false, 500);
  }

  public NoStackException(String message, Throwable cause, int code) {
    super(message, cause, false, code);
  }

  public NoStackException setCode(int code) {
    super.setCode(code);
    return this;
  }



}
