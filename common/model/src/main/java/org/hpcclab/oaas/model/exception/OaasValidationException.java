package org.hpcclab.oaas.model.exception;

import java.util.Collection;

public class OaasValidationException extends StdOaasException{
  public OaasValidationException(String message) {
    super(message);
    setCode(400);
  }

  public static OaasValidationException errorClassCyclicInheritance(Collection<String> path) {
    return new OaasValidationException("Cyclic inheritance detection on %s"
      .formatted(path));
  }
}
