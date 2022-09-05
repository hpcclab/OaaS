package org.hpcclab.oaas.model.exception;

public class FunctionValidationException extends NoStackException {
  public FunctionValidationException(String message) {
    super(message, 400);
  }

  public FunctionValidationException(String message, Throwable cause) {
    super(message, cause, 400);
  }

  public static FunctionValidationException noFunction(String objId, String funcName) {
    return new FunctionValidationException("An object(id='%s') do not have a function(name='%s')."
      .formatted(objId, funcName)
    );
  }


  public static FunctionValidationException accessError(String objId, String funcName) {
    return new FunctionValidationException("An object(id='%s') has a function(name='%s') without PUBLIC access."
      .formatted(objId, funcName)
    );
  }

  public static FunctionValidationException cannotResolveMacro(String ref,
                                                               String reason) {
    if (reason == null) reason = "not found in context or wrong syntax";
    return new FunctionValidationException("Can not resolve '%s' (%s)"
      .formatted(ref, reason));
  }
}
