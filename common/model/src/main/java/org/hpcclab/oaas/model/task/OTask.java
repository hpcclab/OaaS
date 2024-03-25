package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OObject;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OTask {
  public static final String CE_TYPE = "oaas.task";
  String id;
  String partKey;
  OObject main;
  OObject output;
  String funcKey;
  List<OObject> inputs = List.of();
  String allocMainUrl;
  String allocOutputUrl;
  Map<String,String> mainKeys;
  Map<String,String> outputKeys;
  List<String> inputContextKeys = List.of();
  Map<String, String> args;
  String fbName;
  boolean immutable;
  long ts = -1;
}
