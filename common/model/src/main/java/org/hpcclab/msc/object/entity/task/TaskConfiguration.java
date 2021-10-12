package org.hpcclab.msc.object.entity.task;

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
@Embeddable
public class TaskConfiguration implements Serializable {

  Type type;
  String image;
  @ElementCollection
  List<String> commands;
  @ElementCollection
  List<String> containerArgs;
  boolean argsToEnv = true;

  public enum Type{
    LOGICAL, DURABLE, EPHEMERAL
  }
}
