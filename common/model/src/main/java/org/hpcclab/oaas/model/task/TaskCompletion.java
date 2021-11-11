package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@ToString
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {

  String id;
  UUID mainObj;
  UUID outputObj;
  String functionName;
  Status status;
  String startTime;
  String completionTime;
  String requestFile;
  String resourceUrl;
  String debugCondition;
  String debugLog;

  public enum Status {
    SUCCEEDED, FAILED
  }
}
