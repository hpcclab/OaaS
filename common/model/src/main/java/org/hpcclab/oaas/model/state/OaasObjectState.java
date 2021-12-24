package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

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
    @ProtoEnumValue(1)
    FILE,
    @ProtoEnumValue(2)
    FILES,
    @ProtoEnumValue(3)
    SEGMENTABLE,
    @ProtoEnumValue(4)
    RECORD
  }

  public OaasObjectState() {
  }

  @ProtoFactory
  public OaasObjectState(StateType type, String baseUrl, List<String> keys, String records) {
    this.type = type;
    this.baseUrl = baseUrl;
    this.keys = keys;
    this.records = records;
  }

  @ProtoField(1)
  public StateType getType() {
    return type;
  }

  @ProtoField(2)
  public String getBaseUrl() {
    return baseUrl;
  }

  @ProtoField(3)
  public List<String> getKeys() {
    return keys;
  }

  @ProtoField(4)
  public String getRecords() {
    return records;
  }
}
