package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.JsonBytes;

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
  JsonBytes custom = JsonBytes.EMPTY;

  public enum OffloadingMode{
    JSON,PROTOBUF,GRPC
  }
}
