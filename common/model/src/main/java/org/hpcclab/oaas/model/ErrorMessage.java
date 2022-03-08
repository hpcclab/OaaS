package org.hpcclab.oaas.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorMessage {
  String msg;

  public ErrorMessage(String msg) {
    this.msg = msg;
  }
}
