package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObjectState implements Serializable {
  @ProtoField(number = 2, javaType = HashMap.class)
  Map<String,String> overrideUrls;

  @ProtoField(number = 3, javaType = HashMap.class)
  Map<String,String> verIds;


  public OaasObjectState() {
  }

  public OaasObjectState(Map<String, String> overrideUrls,
                         Map<String, String> verIds) {
    this.overrideUrls = overrideUrls;
    this.verIds = verIds;
  }

  @ProtoFactory
  public OaasObjectState(HashMap<String, String> overrideUrls,
                         HashMap<String, String> verIds) {
    this.overrideUrls = overrideUrls;
    this.verIds = verIds;
  }

  public OaasObjectState copy() {
    return new OaasObjectState(
      overrideUrls ==null? null: Map.copyOf(overrideUrls),
      verIds==null? null: Map.copyOf(verIds)
    );
  }
}
