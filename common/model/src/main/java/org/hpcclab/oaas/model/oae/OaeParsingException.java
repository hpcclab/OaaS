package org.hpcclab.oaas.model.oae;

import org.hpcclab.oaas.model.exception.NoStackException;

public class OaeParsingException extends NoStackException {
  public OaeParsingException(String message) {
    super(message, 400);
  }
}
