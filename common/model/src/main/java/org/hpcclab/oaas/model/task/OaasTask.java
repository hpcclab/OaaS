package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.state.OaasObjectState;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasTask {
  String id;
  OaasObject main;
  OaasObject output;
  OaasFunction function;
  List<OaasObject> additionalInputs = List.of();
  String requestFile;
}
