package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.object.DeepOaasObjectDto;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.object.OaasObjectDto;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasTask extends BaseTaskMessage{
//  String id;
  OaasObjectPb main;
  OaasObjectPb output;
  OaasFunctionPb function;
  List<OaasObjectPb> additionalInputs = List.of();
  String requestFile;

  @Override
  public OaasTask setId(String id) {
    this.id = id;
    return this;
  }

  public static String createId(OaasObjectPb outputObj, String requestFile) {
    if (outputObj.getState().getType() == OaasObjectState.StateType.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
//  public static String createId(DeepOaasObjectDto outputObj, String requestFile) {
//    if (outputObj.getState().getType() == OaasObjectState.StateType.SEGMENTABLE )
//      return outputObj.getId().toString() + "/" + requestFile;
//    else
//      return outputObj.getId().toString();
//  }

  public static String createId(String oid, String requestFile) {
    if (requestFile == null) return oid;
    return oid+'/' + requestFile;
  }
}
