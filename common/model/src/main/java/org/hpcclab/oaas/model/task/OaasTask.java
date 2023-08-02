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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasTask implements TaskDetail {
  public static final String CE_TYPE = "oaas.task";
  TaskIdentity id;
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
  boolean immutable;
  long ts = -1;

  @Override
  @JsonIgnore
  public String getVid() {
    return id.getVid();
  }
}
