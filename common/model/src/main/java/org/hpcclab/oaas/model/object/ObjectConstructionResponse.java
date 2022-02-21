package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectConstructionResponse {
  OaasObject object;
  Map<String, String> uploadUrls;

  public ObjectConstructionResponse(OaasObject object, Map<String, String> uploadUrls) {
    this.object = object;
    this.uploadUrls = uploadUrls;
  }
}
