package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext {
  OaasObject output;
//  OaasClass outputCls;
  OaasObject main;
//  OaasClass mainCls;
  Map<String,OaasObject> mainRefs;
  OaasFunction function;
  List<OaasObject> inputs = List.of();
//  List<OaasClass> inputCls = List.of();
}
