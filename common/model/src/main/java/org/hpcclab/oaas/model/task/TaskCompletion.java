package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;

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

  @ProtoField(value = 4, javaType = ObjectNode.class)
  ObjectNode embeddedRecord;
  Map<String,String> extensions;

  @JsonIgnore
  long cmpTs = -1;
  @JsonIgnore
  long smtTs = -1;


  public TaskCompletion() {
  }

  @ProtoFactory
  public TaskCompletion(String id, boolean success, String errorMsg, ObjectNode embeddedRecord) {
    this.id = id;
    this.success = success;
    this.errorMsg = errorMsg;
    this.embeddedRecord = embeddedRecord;
  }

  public TaskCompletion(String id,
                        boolean success,
                        String errorMsg,
                        ObjectNode embeddedRecord,
                        Map<String,String> extensions,
                        long smtTs,
                        long cmpTs) {
    this.id = id;
    this.success = success;
    this.errorMsg = errorMsg;
    this.embeddedRecord = embeddedRecord;
    this.extensions = extensions;
    this.smtTs = smtTs;
    this.cmpTs = cmpTs;
  }
}
