package org.hpcclab.oaas.model.exception;

import org.hpcclab.oaas.model.object.OObject;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvocationException extends StdOaasException {
  String invId;
  boolean retryable = true;
  boolean connErr = false;

  public InvocationException(String message, Throwable cause) {
    super(message, cause, true, 500);
  }

  public InvocationException(String message) {
    super(message, null, true, 500);
  }

  public InvocationException(String message, int code) {
    super(message, code);
  }

  public InvocationException(String message, Throwable cause, int code) {
    super(message, cause, true, code);
  }

  public InvocationException(Throwable cause, String invId) {
    super(null, cause);
    this.invId = invId;
  }

  public static InvocationException detectConcurrent(Throwable e) {
    return new InvocationException("Detect concurrent update in the same object", e,
      HttpURLConnection.HTTP_CONFLICT);
  }

  public static InvocationException notReady(List<Map.Entry<OObject, OObject>> waiting,
                                             List<OObject> failed) {
    return new InvocationException("Dependencies are not ready. {waiting:[%s], failed:[%s]}"
      .formatted(
        waiting.stream()
          .map(entry -> entry.getKey().getId() + ">>" + entry.getValue().getId())
          .collect(Collectors.joining(", ")),
        failed.stream()
          .map(OObject::getId)
          .collect(Collectors.joining(", "))),
      409);
  }

  public static InvocationException connectionErr(Throwable e) {
    var ex = new InvocationException("Connection Error", e, HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
    ex.connErr = true;
    return ex;
  }

  public boolean isRetryable() {
    return retryable;
  }

  public void setRetryable(boolean retryable) {
    this.retryable = retryable;
  }

  public boolean isConnErr() {
    return connErr;
  }

  public String getInvId() {
    return invId;
  }
}
