package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OOUpdate;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class TaskCompletion {
  String id;
  boolean success;
  String errorMsg;
  Map<String, String> ext;
  OOUpdate main;
  OOUpdate output;
  @JsonIgnore
  @Builder.Default
  long cptTs = -1;
  @JsonIgnore
  @Builder.Default
  long smtTs = -1;
  ObjectNode body;
  @Builder.Default
  List<InvocationRequest> invokes = List.of();


  public TaskCompletion() {
  }

  public TaskCompletion(String id,
                        boolean success,
                        String errorMsg,
                        Map<String, String> ext,
                        OOUpdate main,
                        OOUpdate out,
                        long cptTs,
                        long smtTs,
                        ObjectNode body,
                        List<InvocationRequest> invokes) {
    this.id = id;
    this.success = success;
    this.errorMsg = errorMsg;
    this.ext = ext;
    this.main = main;
    this.output = out;
    this.cptTs = cptTs;
    this.smtTs = smtTs;
    this.body = body;
    this.invokes = invokes;
  }


  public static TaskCompletion error(String id,
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
      smtTs,
      null,
      null
    );
  }

  public List<InvocationRequest> getInvokes() {
    if (invokes==null) invokes = List.of();
    return invokes;
  }
}
