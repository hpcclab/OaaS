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


  public static NoStackException notFoundObject400(String uuid) {
    return notFoundObject(uuid, 400);
  }
  public static NoStackException notFoundObject(String uuid, int code) {
    return new NoStackException("Not found object(id='" + uuid + "')", code);
  }


  public static NoStackException notFoundCls400(String name) {
    return notFoundCls(name, 400);
  }
  public static NoStackException notFoundCls(String name, int code) {
    return new NoStackException("Not found class(name='" + name + "')", code);
  }

  public static NoStackException notFoundFunc400(String name) {
    return notFoundFunc(name, 400);
  }
  public static NoStackException notFoundFunc(String name, int code) {
    return new NoStackException("Not found function(name='" + name + "')", code);
  }

  public static StdOaasException notImplemented() {
    return new StdOaasException("The request is involve in the operation that not implemented", 501);
  }


}
