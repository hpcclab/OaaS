package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskConfiguration {

  private Type type;
  private String image;
  private List<String> commands;
  private List<String> containerArgs;
  private List<String> outputFileNames;
  private Map<String, String> provisionConfig = Map.of();
  private boolean argsToEnv = true;

  public enum Type {
    LOGICAL, DURABLE, EPHEMERAL
  }
}
