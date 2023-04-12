package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.KvPair;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObjectState implements Serializable {
  @ProtoField(number = 2, javaType = HashMap.class)
  Set<KvPair> overrideUrls;

  @ProtoField(number = 3, javaType = HashMap.class)
  Set<KvPair> verIds;


  public OaasObjectState() {
  }

  @ProtoFactory
  public OaasObjectState(Set<KvPair> overrideUrls, Set<KvPair> verIds) {
    this.overrideUrls = overrideUrls;
    this.verIds = verIds;
  }

  public OaasObjectState copy() {
    return new OaasObjectState(
      overrideUrls ==null? null: Set.copyOf(overrideUrls),
      verIds==null? null: Set.copyOf(verIds)
    );
  }

  public String findVerId(String key) {
    if (verIds == null) return null;
    return verIds.stream()
      .filter(p -> p.getKey().equals(key))
      .map(KvPair::getVal)
      .findFirst().orElse(null);
  }
  public String findOverrideUrl(String key) {
    if (overrideUrls == null) return null;
    return overrideUrls.stream()
      .filter(p -> p.getKey().equals(key))
      .map(KvPair::getVal)
      .findFirst().orElse(null);
  }
}
