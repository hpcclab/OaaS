package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author Pawissanutt
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OFunctionConfig {
  Map<String, String> offloadingConfig;
  OffloadingMode offloadingMode;
  boolean http2;
  Map<String, Object> custom = Map.of();
  String apiPath = "";
  String staticUrl;

  public enum OffloadingMode{
    JSON,PROTOBUF,GRPC
  }
}
