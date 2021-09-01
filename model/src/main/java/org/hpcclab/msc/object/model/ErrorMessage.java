package org.hpcclab.msc.object.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ErrorMessage {
  String msg;
}
