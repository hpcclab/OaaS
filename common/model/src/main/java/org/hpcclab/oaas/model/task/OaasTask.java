package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasTask {
  String id;
  OaasObject main;
  OaasObject output;
  @JsonIgnore
  OaasFunction function;
  List<OaasObject> inputs = List.of();
  String allocOutputUrl;
  Map<String,String> mainKeys;
  List<Map<String,String>> inputKeys = List.of();
}
