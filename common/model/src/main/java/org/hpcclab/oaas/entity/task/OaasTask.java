package org.hpcclab.oaas.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.state.OaasObjectState;
import org.hpcclab.oaas.model.OaasFunctionDto;
import org.hpcclab.oaas.model.OaasObjectDto;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasTask {
  String id;
  OaasObjectDto main;
  OaasObjectDto output;
  OaasFunctionDto function;
  List<OaasObjectDto> additionalInputs = List.of();
  String requestFile;

  public static String createId(OaasObject outputObj, String requestFile) {
    if (outputObj.getState().getType() == OaasObjectState.StateType.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
}
