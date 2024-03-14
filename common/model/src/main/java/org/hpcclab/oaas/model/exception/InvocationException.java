package org.hpcclab.oaas.model.exception;

import org.hpcclab.oaas.model.object.OObject;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
