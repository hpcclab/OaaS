package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.ObjectUpdate;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskCompletion {
  String id;
  String vId;
  boolean success;
  String errorMsg;
  //  ObjectNode embeddedRecord;
  Map<String, String> ext;

  ObjectUpdate main;
  ObjectUpdate output;

  @JsonIgnore
  long cptTs = -1;
  @JsonIgnore
  long smtTs = -1;


  public TaskCompletion() {
  }


  public TaskCompletion(String id,
                        String vId,
                        boolean success,
                        String errorMsg,
                        Map<String, String> ext,
                        ObjectUpdate main,
                        ObjectUpdate out,
                        long cptTs,
                        long smtTs) {
    this.id = id;
    this.vId = vId;
    this.success = success;
    this.errorMsg = errorMsg;
    this.ext = ext;
    this.main = main;
    this.output = out;
    this.cptTs = cptTs;
    this.smtTs = smtTs;
  }

  public static TaskCompletion error(String id,
                                     String vId,
                                     String errorMsg,
                                     long cptTs,
                                     long smtTs) {
    return new TaskCompletion(
      id,
      vId,
      false,
      errorMsg,
      null,
      null,
      null,
      cptTs,
      smtTs
    );
  }
}
