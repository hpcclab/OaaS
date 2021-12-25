package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.UUID;

@Data
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

  @ProtoField(1)
  public String getId() {
    return id;
  }

  @ProtoField(2)
  public UUID getMainObj() {
    return mainObj;
  }

  @ProtoField(3)
  public UUID getOutputObj() {
    return outputObj;
  }

  @ProtoField(4)
  public String getFunctionName() {
    return functionName;
  }

  @ProtoField(5)
  public TaskStatus getStatus() {
    return status;
  }

  @ProtoField(6)
  public String getStartTime() {
    return startTime;
  }

  @ProtoField(7)
  public String getCompletionTime() {
    return completionTime;
  }

  @ProtoField(8)
  public String getRequestFile() {
    return requestFile;
  }

  @ProtoField(9)
  public String getResourceUrl() {
    return resourceUrl;
  }

  @ProtoField(10)
  public String getDebugLog() {
    return debugLog;
  }
}
