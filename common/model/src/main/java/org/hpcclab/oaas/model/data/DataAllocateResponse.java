package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAllocateResponse {
  String id;
  Map<String, String> urlKeys;

  public DataAllocateResponse(String id, Map<String, String> urlKeys) {
    this.id = id;
    this.urlKeys = urlKeys;
  }
}
