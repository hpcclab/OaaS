package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectState implements Serializable {
  StateType type;
  String baseUrl;
  List<String> keys;
  @JsonRawValue
  String records;

  public enum StateType {
    FILE, FILES, SEGMENTABLE, RECORD
  }
}
