package org.hpcclab.oaas.model.exception;

public class StdOaasException extends RuntimeException {
  final int code;

  public StdOaasException(int code) {
    this(null, null, true, code);
  }


  public StdOaasException(Throwable cause) {
    this(null, cause, true, 500);
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



  public static StdOaasException notFoundObject400(String uuid) {
    return notFoundObject(uuid, 400);
  }
  public static StdOaasException notFoundObject(String uuid, int code) {
    return new StdOaasException("Not found object(id='" + uuid + "')", code);
  }


  public static StdOaasException notFoundCls400(String name) {
    return notFoundCls(name, 400);
  }
  public static StdOaasException notFoundCls(String name, int code) {
    return new StdOaasException("Not found class(name='" + name + "')", code);
  }

  public static StdOaasException notFoundFunc(String name, int code) {
    return new StdOaasException("Not found function(name='" + name + "')", code);
  }

  public static StdOaasException notImplemented() {
    return new StdOaasException("The request is involve in the operation that not implemented", 501);
  }

  public static StdOaasException notKeyInObj(String oid, int code) {
    return new StdOaasException("No such key exist in object(%s)".formatted(oid), code);
  }

  public static StdOaasException format(String template, Object... args) {
    return new StdOaasException(template.formatted(args));
  }
}
