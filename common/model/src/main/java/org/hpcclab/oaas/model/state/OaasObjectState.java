package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonAlias;
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
//  @ProtoField(1)
//  StateType type;
  @ProtoField(2)
  String stateId;
  @ProtoField(number = 3, javaType = HashMap.class)
  Map<String,String> overrideUrls;


  public OaasObjectState() {
  }

  public OaasObjectState(String stateId,
                         Map<String, String> overrideUrls) {
    this.stateId = stateId;
    this.overrideUrls = overrideUrls;
  }

  @ProtoFactory
  public OaasObjectState(String stateId,
                         HashMap<String, String> overrideUrls) {
//    this.type = type;
    this.stateId = stateId;
    this.overrideUrls = overrideUrls;
  }

  public OaasObjectState copy() {
    return new OaasObjectState(
      stateId,
      overrideUrls ==null? null: Map.copyOf(overrideUrls)
    );
  }
}
