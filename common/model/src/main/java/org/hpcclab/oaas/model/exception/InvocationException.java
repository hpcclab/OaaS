package org.hpcclab.oaas.model.exception;

import java.net.HttpURLConnection;

public class InvocationException extends StdOaasException {
  final String invId;

  public InvocationException(String message) {
    this(message, null, 500, null);
  }

  public InvocationException(String message, Throwable cause) {
    this(message, cause, 500, null);
  }

  public InvocationException(String message, int code) {
    this(message, null, code, null);
  }

  public InvocationException(String message, Throwable cause, int code) {
    this(message, cause, code, null);
  }

  public InvocationException(String message, Throwable cause, int code, String invId) {
    super(message, cause, true, code);
    this.invId = invId;
  }

  public static InvocationException detectConcurrent(Throwable e) {
    return new InvocationException("Detect concurrent update in the same object", e,
      HttpURLConnection.HTTP_CONFLICT);
  }

  public static InvocationException connectionErr(Throwable e) {
    return new InvocationException("Connection Error", e,
      HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
  }

  public static StdOaasException notFoundFnInCls(String fb, String cls) {
    return new StdOaasException("Not found FunctionBinding(" + fb + ") in Class(" + cls + ")", 400);
  }


  public String getInvId() {
    return invId;
  }
}
