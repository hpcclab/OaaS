package org.hpcclab.oaas.model.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;

public class DataAccessException extends StdOaasException {
  public DataAccessException(String message) {
    super(message);
  }

  public DataAccessException(int code) {
    super(code);
  }

  public DataAccessException(String message, int code) {
    super(message, code);
  }

  public DataAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public DataAccessException(String message, Throwable cause, boolean writableStack, int code) {
    super(message, cause, writableStack, code);
  }

  public DataAccessException(IOException e) {
    super(null, e, true, 500);
  }

  public static DataAccessException concurrentMod(){
    throw new DataAccessException("Detected concurrent modification", HttpResponseStatus.CONFLICT.code());
  }
}
