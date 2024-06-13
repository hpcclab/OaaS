package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OTask {
  public static final String CE_TYPE = "oaas.task";
  String id;
  String partKey;
  GOObject main;
  GOObject output;
  String funcKey;
  String allocMainUrl;
  String allocOutputUrl;
  Map<String, String> mainGetKeys;
  Map<String, String> mainPutKeys;
  Map<String, String> outputKeys;
  Map<String, String> args;
  JsonBytes reqBody;
  String fbName;
  boolean immutable;
  long ts = -1;
}
