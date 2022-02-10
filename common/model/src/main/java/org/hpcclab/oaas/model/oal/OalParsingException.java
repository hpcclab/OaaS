package org.hpcclab.oaas.model.oal;

import org.hpcclab.oaas.model.exception.NoStackException;

public class OalParsingException extends NoStackException {
  public OalParsingException(String message) {
    super(message, 400);
  }
}
