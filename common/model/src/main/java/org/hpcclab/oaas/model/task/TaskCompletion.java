package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
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
  String errorMsg;
  @JsonRawValue
  @ProtoField(4)
  String embeddedRecord;

  @JsonRawValue
  @ProtoField(5)
  String mergedRecord;

  public TaskCompletion() {
  }

  @ProtoFactory
  public TaskCompletion(String id, boolean success, String errorMsg, String embeddedRecord, String mergedRecord) {
    this.id = id;
    this.success = success;
    this.errorMsg = errorMsg;
    this.embeddedRecord = embeddedRecord;
    this.mergedRecord = mergedRecord;
  }

  @JsonSetter
  public TaskCompletion setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
    return this;
  }

  public TaskCompletion setEmbeddedRecord(String embeddedState) {
    this.embeddedRecord = embeddedState;
    return this;
  }

  @JsonSetter
  public TaskCompletion setMergedRecord(JsonNode val) {
    this.mergedRecord = val.toString();
    return this;
  }

  public TaskCompletion setMergedRecord(String mergedRecord) {
    this.mergedRecord = mergedRecord;
    return this;
  }
}
