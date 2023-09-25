package org.hpcclab.oaas.model.oal;

import org.hpcclab.oaas.model.exception.StdOaasException;

public class OalParsingException extends StdOaasException {
  public OalParsingException(String message) {
    super(message, 400);
  }
}
