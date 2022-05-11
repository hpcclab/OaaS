package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectConstructResponse {
  OaasObject object;
  Map<String, String> uploadUrls;
  List<ObjectConstructResponse> streams;

  public ObjectConstructResponse() {
  }

  public ObjectConstructResponse(OaasObject object, Map<String, String> uploadUrls) {
    this.object = object;
    this.uploadUrls = uploadUrls;
  }

  public ObjectConstructResponse(OaasObject object, Map<String, String> uploadUrls, List<ObjectConstructResponse> streams) {
    this.object = object;
    this.uploadUrls = uploadUrls;
    this.streams = streams;
  }
}
