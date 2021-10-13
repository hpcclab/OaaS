package org.hpcclab.msc.object.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Embeddable
public class Task {
  @Id
  String id;
  @ManyToOne(fetch = FetchType.EAGER)
  OaasObject main;
  @ManyToOne(fetch = FetchType.EAGER)
  OaasObject output;
  @ManyToOne(fetch = FetchType.EAGER)
  OaasFunction function;
  String requestFile;
  @ElementCollection
  Map<String, String> args= Map.of();
  @ManyToMany(fetch = FetchType.EAGER)
  @OrderColumn
  List<OaasObject> additionalInputs = List.of();

  public static String createId(OaasObject outputObj, String requestFile) {
    if (outputObj.getState().getType() == OaasObjectState.StateType.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
}
