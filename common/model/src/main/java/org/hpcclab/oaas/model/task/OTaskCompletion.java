package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.object.OOUpdate;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class OTaskCompletion {
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
  JsonBytes body;
  @Builder.Default
  List<InvocationRequest> invokes = List.of();


  public OTaskCompletion() {
  }

  public OTaskCompletion(String id,
                         boolean success,
                         String errorMsg,
                         Map<String, String> ext,
                         OOUpdate main,
                         OOUpdate out,
                         long cptTs,
                         long smtTs,
                         JsonBytes body,
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


  public static OTaskCompletion error(String id,
                                      String errorMsg,
                                      long cptTs,
                                      long smtTs) {
    return new OTaskCompletion(
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
