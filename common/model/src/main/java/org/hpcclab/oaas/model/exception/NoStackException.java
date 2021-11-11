package org.hpcclab.oaas.model.exception;

import java.util.UUID;

public class NoStackException extends RuntimeException{

  int code = 500;

  public NoStackException(String message) {
    super(message, null, true, true);
  }


  public NoStackException(String message, int code) {
    super(message, null, true, true);
    code = code;
  }

  public NoStackException(String message, Throwable cause) {
    super(message, cause, false, false);
  }

  public int getCode() {
    return code;
  }

  public NoStackException setCode(int code) {
    this.code = code;
    return this;
  }

  public static NoStackException notFoundObject400(UUID uuid) {
    return new NoStackException("Not found object(id='" + uuid + "')", 400);
  }


  public static NoStackException notFoundCls400(String name) {
    return new NoStackException("Not found class(name='" + name + "')", 400);
  }
}
