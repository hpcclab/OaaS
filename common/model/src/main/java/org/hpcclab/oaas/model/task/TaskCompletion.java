package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;

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
  TaskStatus status;
  String startTime;
  String completionTime;
  String requestFile;
  String resourceUrl;
  String debugLog;

  public TaskCompletion() {
  }

  @ProtoFactory
  public TaskCompletion(String id, UUID mainObj, UUID outputObj, String functionName, TaskStatus status, String startTime, String completionTime, String requestFile, String resourceUrl, String debugLog) {
    this.id = id;
    this.mainObj = mainObj;
    this.outputObj = outputObj;
    this.functionName = functionName;
    this.status = status;
    this.startTime = startTime;
    this.completionTime = completionTime;
    this.requestFile = requestFile;
    this.resourceUrl = resourceUrl;
    this.debugLog = debugLog;
  }
}
