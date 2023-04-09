package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.HasKey;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashSet;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ObjectInvNode implements HasKey {
  @JsonProperty("_key")
  @ProtoField(1)
  String key;
  @ProtoField(2)
  Set<String> nextInv;

  public ObjectInvNode() {
  }

  public ObjectInvNode(String key) {
    this.key = key;
    this.nextInv = new HashSet<>();
  }

  @ProtoFactory
  public ObjectInvNode(String key, Set<String> nextInv) {
    this.key = key;
    this.nextInv = nextInv;
  }

  public Set<String> getNextInv() {
    if (nextInv== null) nextInv = Sets.mutable.empty();
    return nextInv;
  }
}
