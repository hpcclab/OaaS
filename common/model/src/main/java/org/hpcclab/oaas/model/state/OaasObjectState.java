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
  String stateId;
  Map<String,String> overrideUrls;

  public enum StateType {
    @ProtoEnumValue(2)
    FILES,
    @ProtoEnumValue(3)
    COLLECTION,
  }

  public OaasObjectState() {
  }

  public OaasObjectState(StateType type,
                         String stateId,
                         Map<String, String> overrideUrls) {
    this.type = type;
    this.stateId = stateId;
    this.overrideUrls = overrideUrls;
  }

  @ProtoFactory
  public OaasObjectState(StateType type,
                         String stateId,
                         HashMap<String, String> overrideUrls) {
    this.type = type;
    this.stateId = stateId;
    this.overrideUrls = overrideUrls;
  }

  @ProtoField(1)
  public StateType getType() {
    return type;
  }


  @ProtoField(3)
  public String getStateId() {
    return stateId;
  }

  @ProtoField(number = 4, javaType = HashMap.class)
  public Map<String, String> getOverrideUrls() {
    return overrideUrls;
  }
}
