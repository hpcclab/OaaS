package org.hpcclab.oaas.model.exception;

public class FunctionValidationException extends OaasValidationException {
  public FunctionValidationException(String message) {
    super(message);
  }

  public FunctionValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public static FunctionValidationException noFunction(String cls, String funcName) {
    return new FunctionValidationException("An object(cls='%s') do not have a function(name='%s')."
      .formatted(cls, funcName)
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

  public static FunctionValidationException format(String template, Object... params) {
    return new FunctionValidationException(template.formatted(params));
  }
}
