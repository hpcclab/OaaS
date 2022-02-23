package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObjectState implements Serializable {
  StateType type;
  String baseUrl;
  @Deprecated(forRemoval = true)
  List<String> keys;
  Map<String,String> overrideUrls;

  public enum StateType {
    @ProtoEnumValue(1)
    FILE,
    @ProtoEnumValue(2)
    FILES,
    @ProtoEnumValue(3)
    COLLECTION,
    @Deprecated(forRemoval = true)
    @ProtoEnumValue(4)
    RECORD
  }

  public OaasObjectState() {
  }

  public OaasObjectState(StateType type, String baseUrl, List<String> keys, Map<String, String> overrideUrls) {
    this.type = type;
    this.baseUrl = baseUrl;
    this.keys = keys;
    this.overrideUrls = overrideUrls;
  }

  @ProtoFactory
  public OaasObjectState(StateType type, String baseUrl, List<String> keys, HashMap<String, String> overrideUrls) {
    this.type = type;
    this.baseUrl = baseUrl;
    this.keys = keys;
    this.overrideUrls = overrideUrls;
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

  @ProtoField(number = 4, javaType = HashMap.class)
  public Map<String, String> getOverrideUrls() {
    return overrideUrls;
  }
}
