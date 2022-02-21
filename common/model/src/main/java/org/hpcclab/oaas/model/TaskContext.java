package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext {
  OaasObject output;
  OaasClass outputCls;
  OaasObject main;
  OaasClass mainCls;
  OaasFunction function;
  List<OaasObject> inputs = List.of();
  List<OaasClass> inputCls = List.of();
}
