package org.hpcclab.oaas.invocation.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

/**
 * @author Pawissanutt
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class InvocationLog {
  @JsonProperty("_key")
  @ProtoField(1)
  String key;
  @ProtoField(2)
  String fb;
  @ProtoField(3)
  String main;
  @ProtoField(4)
  String cls;
  @ProtoField(5)
  DSMap args;
  @ProtoField(6)
  List<String> inputs;
  @ProtoField(7)
  String outId;
  @ProtoField(8)
  String originator;
  @ProtoField(9)
  InvocationStatus status = InvocationStatus.QUEUE;
  @ProtoField(value = 10, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long queTs;
  @ProtoField(value = 11, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long smtTs;
  @ProtoField(value = 12, defaultValue = "-1")
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  long cptTs;

  public void updateStatus(TaskCompletion completion) {
    if (completion.isSuccess()) {
      status = InvocationStatus.SUCCEEDED;
    } else
      status = InvocationStatus.FAILED;
    if (completion.getCptTs() > 0) {
      cptTs = completion.getCptTs();
    } else {
      cptTs = System.currentTimeMillis();
    }
    if (completion.getSmtTs() > 0) {
      smtTs = completion.getSmtTs();
    }
  }

  public InvocationStats extractStats() {
    return new InvocationStats(queTs, smtTs, cptTs);
  }
}
