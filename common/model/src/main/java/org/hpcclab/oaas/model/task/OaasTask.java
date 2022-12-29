package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.OaasObject;

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
  @Deprecated(forRemoval = true)
  List<Map<String,String>> inputKeys = List.of();
  List<String> inputContextKeys = List.of();
  Map<String, String> args;
  long ts = -1;
}
