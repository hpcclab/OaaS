package org.hpcclab.oaas.model.exception;

import java.util.UUID;

public class NoStackException extends RuntimeException{

  public static final NoStackException INSTANCE = new NoStackException("INSTANCE",500);

  int code;

  public NoStackException(int code) {
    this.code = code;
  }

  public NoStackException(String message) {
    this(message, null, 500);
  }


  public NoStackException(String message, int code) {
    this(message, null, code);
  }

  public NoStackException(String message, Throwable cause) {
    this(message, cause, 500);
  }

  public NoStackException(String message, Throwable cause, int code) {
    super(message, cause, false, false);
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public NoStackException setCode(int code) {
    this.code = code;
    return this;
  }

  public static NoStackException notFoundObject400(UUID uuid) {
    return notFoundObject(uuid, 400);
  }
  public static NoStackException notFoundObject(UUID uuid, int code) {
    return new NoStackException("Not found object(id='" + uuid + "')", code);
  }


  public static NoStackException notFoundCls400(String name) {
    return new NoStackException("Not found class(name='" + name + "')", 400);
  }

  public static NoStackException notFoundFunc400(String name) {
    return new NoStackException("Not found function(name='" + name + "')", 400);
  }


}
