package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.model.exception.StdOaasException;

/**
 * @author Pawissanutt
 */
public class DataflowParseException extends StdOaasException {
  public DataflowParseException() {
    super(400);
  }

  public DataflowParseException(String message, int code) {
    super(message, 400);
  }
}
