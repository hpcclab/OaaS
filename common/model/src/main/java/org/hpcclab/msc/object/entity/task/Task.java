package org.hpcclab.msc.object.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
  String id;
  OaasObject main;
  OaasObject output;
  OaasFunction function;
  String requestFile;
  Map<String, String> args= Map.of();
  List<OaasObject> additionalInputs = List.of();

  public static String createId(OaasObject outputObj, String requestFile) {
    if (outputObj.getState().getType() == OaasObjectState.Type.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
}
