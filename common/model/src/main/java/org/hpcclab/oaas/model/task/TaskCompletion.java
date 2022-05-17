package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
  @ProtoField(1)
  String id;
  @ProtoField(value = 2, defaultValue = "true")
  boolean success;
  @ProtoField(3)
  String debugLog;
  @JsonRawValue
  @ProtoField(4)
  String embeddedRecord;

  public TaskCompletion() {
  }

  @ProtoFactory
  public TaskCompletion(String id, boolean success, String debugLog, String embeddedRecord) {
    this.id = id;
    this.success = success;
    this.debugLog = debugLog;
    this.embeddedRecord = embeddedRecord;
  }

  public TaskCompletion setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
    return this;
  }

  public TaskCompletion setEmbeddedRecord(String embeddedState) {
    this.embeddedRecord = embeddedState;
    return this;
  }
}
