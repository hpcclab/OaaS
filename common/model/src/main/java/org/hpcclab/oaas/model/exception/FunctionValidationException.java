package org.hpcclab.oaas.model.exception;

public class FunctionValidationException extends NoStackException{
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
}
