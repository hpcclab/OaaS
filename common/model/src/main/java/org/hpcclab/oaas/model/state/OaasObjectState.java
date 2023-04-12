package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.StringKvPair;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObjectState implements Serializable {
  @ProtoField(number = 2, javaType = HashMap.class)
  Set<StringKvPair> overrideUrls;

  @ProtoField(number = 3, javaType = HashMap.class)
  Set<StringKvPair> verIds;


  public OaasObjectState() {
  }

  @ProtoFactory
  public OaasObjectState(Set<StringKvPair> overrideUrls, Set<StringKvPair> verIds) {
    this.overrideUrls = overrideUrls;
    this.verIds = verIds;
  }

  public OaasObjectState copy() {
    return new OaasObjectState(
      overrideUrls ==null? null: Set.copyOf(overrideUrls),
      verIds==null? null: Set.copyOf(verIds)
    );
  }
}
