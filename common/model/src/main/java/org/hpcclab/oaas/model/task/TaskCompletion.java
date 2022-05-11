package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
  String id;
//  String functionName;
  TaskStatus status;
  long startTime = -1;
  long completionTime = -1;
  String debugLog;
  @JsonRawValue
  String embeddedRecord;

  public TaskCompletion() {
  }

  @ProtoFactory
  public TaskCompletion(String id,
//                        String functionName,
                        TaskStatus status,
                        long startTime,
                        long completionTime,
                        String debugLog,
                        String embeddedRecord) {
    this.id = id;
    this.status = status;
    this.startTime = startTime;
    this.completionTime = completionTime;
    this.debugLog = debugLog;
    this.embeddedRecord = embeddedRecord;
  }

  @ProtoField(1)
  public String getId() {
    return id;
  }

  @ProtoField(5)
  public TaskStatus getStatus() {
    return status;
  }

  @ProtoField(value = 6,defaultValue = "-1")
  public long getStartTime() {
    return startTime;
  }

  @ProtoField(value = 7,defaultValue = "-1")
  public long getCompletionTime() {
    return completionTime;
  }

  @ProtoField(10)
  public String getDebugLog() {
    return debugLog;
  }
  @ProtoField(11)
  public String getEmbeddedRecord() {
    return embeddedRecord;
  }

  public void setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
  }

  public void setEmbeddedRecord(String embeddedRecord) {
    this.embeddedRecord = embeddedRecord;
  }
}
