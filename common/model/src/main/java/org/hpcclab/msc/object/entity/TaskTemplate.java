package org.hpcclab.msc.object.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskTemplate {

  Type type;
  Map<String, String> values;
  boolean argsToEnv = true;

  public enum Type{
    LOGICAL, HTTP, DURABLE_WORKER, EPHEMERAL_WORKER
  }
}
