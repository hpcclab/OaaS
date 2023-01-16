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
public class OaasTask implements TaskDetail{
  String id;
  String vId;
  String partKey;
  OaasObject main;
  OaasObject output;
  String funcKey;
  @JsonIgnore
  OaasFunction function;
  List<OaasObject> inputs = List.of();
  String allocMainUrl;
  String allocOutputUrl;
  Map<String,String> mainKeys;
  List<String> inputContextKeys = List.of();
  Map<String, String> args;
  String fbName;
  long ts = -1;
}
