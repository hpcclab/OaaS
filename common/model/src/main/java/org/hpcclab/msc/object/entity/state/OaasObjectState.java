package org.hpcclab.msc.object.entity.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectState {
  StateType type;
  String baseUrl;
  String file;
  List<String> files;
  @JsonRawValue
  String records;
  String groupId;

  public enum StateType {
    FILE, FILES, SEGMENTABLE, RECORD
  }
}
