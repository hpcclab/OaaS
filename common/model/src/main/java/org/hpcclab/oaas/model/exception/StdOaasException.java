package org.hpcclab.oaas.model.exception;

public class StdOaasException extends RuntimeException {
  int code;

  public StdOaasException(int code) {
    this(null, null, true, code);
  }

  public StdOaasException(String message) {
    this(message, null, true, 500);
  }


  public StdOaasException(String message, int code) {
    this(message, null, true, code);
  }

  public StdOaasException(String message, Throwable cause) {
    this(message, cause, true, 500);
  }

  public StdOaasException(String message, Throwable cause, boolean writableStack, int code) {
    super(message, cause, false, writableStack);
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public StdOaasException setCode(int code) {
    this.code = code;
    return this;
  }
}
