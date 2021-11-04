package org.hpcclab.oaas.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskConfiguration {

  Type type;
  String image;
  List<String> commands;
  List<String> containerArgs;
  List<String> outputFileNames;
  boolean argsToEnv = true;

  public enum Type{
    LOGICAL, DURABLE, EPHEMERAL
  }
}
