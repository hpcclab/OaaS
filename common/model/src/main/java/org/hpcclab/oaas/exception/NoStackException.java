package org.hpcclab.oaas.exception;

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
}
