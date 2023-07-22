package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.ObjectUpdate;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
  @JsonUnwrapped
  TaskIdentity id;
  boolean success;
  String errorMsg;
  Map<String, String> ext;
  ObjectUpdate main;
  ObjectUpdate output;
  @JsonIgnore
  long cptTs = -1;
  @JsonIgnore
  long smtTs = -1;
  ObjectNode body;


  public TaskCompletion() {
  }

  public TaskCompletion(TaskIdentity id,
                        boolean success,
                        String errorMsg,
                        Map<String, String> ext,
                        ObjectUpdate main,
                        ObjectUpdate out,
                        long cptTs,
                        long smtTs) {
    this.id = id;
    this.success = success;
    this.errorMsg = errorMsg;
    this.ext = ext;
    this.main = main;
    this.output = out;
    this.cptTs = cptTs;
    this.smtTs = smtTs;
  }


  public static TaskCompletion error(TaskIdentity id,
                                     String errorMsg,
                                     long cptTs,
                                     long smtTs) {
    return new TaskCompletion(
      id,
      false,
      errorMsg,
      null,
      null,
      null,
      cptTs,
      smtTs
    );
  }

  public TaskIdentity getId() {
    if (id==null) id = new TaskIdentity();
    return id;
  }
}
