package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
  String mainObj;
  String outputObj;
  String functionName;
  Status status;
  String startTime;
  String completionTime;
  String debugMessage;

  public enum Status {
    SUCCEEDED, FAILED
  }
}
