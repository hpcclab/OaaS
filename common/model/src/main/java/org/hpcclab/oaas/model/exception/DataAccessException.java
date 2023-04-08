package org.hpcclab.oaas.model.exception;

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

  public static DataAccessException concurrentMod(){
    throw new DataAccessException("Detected concurrent modification");
  }
}
