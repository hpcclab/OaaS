package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OTask {
  public static final String CE_TYPE = "oaas.task";
  String id;
  String partKey;
  JOObject main;
  JOObject output;
  String funcKey;
  //List<POObject> inputs = List.of();
  //List<String> inputContextKeys = List.of();
  String allocMainUrl;
  String allocOutputUrl;
  Map<String,String> mainKeys;
  Map<String,String> outputKeys;
  Map<String, String> args;
  ObjectNode reqBody;
  String fbName;
  boolean immutable;
  long ts = -1;
}
