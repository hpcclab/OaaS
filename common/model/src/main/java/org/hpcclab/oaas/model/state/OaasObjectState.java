package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObjectState {
  @ProtoField(number = 2)
  Map<String, String> overrideUrls;

  @ProtoField(number = 3)
  Map<String, String> verIds;


  public OaasObjectState() {
  }

  @ProtoFactory
  public OaasObjectState(Map<String, String> overrideUrls, Map<String, String> verIds) {
    this.overrideUrls = overrideUrls;
    this.verIds = verIds;
  }

  public OaasObjectState copy() {
    return new OaasObjectState(
      overrideUrls==null ? null: Map.copyOf(overrideUrls),
      verIds==null ? null:Map.copyOf(verIds)
    );
  }

  public String findVerId(String key) {
    if (verIds==null) return null;
    return verIds.get(key);
  }

  public String findOverrideUrl(String key) {
    if (overrideUrls==null) return null;
    return overrideUrls.get(key);
  }

}
