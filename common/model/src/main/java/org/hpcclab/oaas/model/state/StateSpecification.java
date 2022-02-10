package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class StateSpecification implements Serializable {
  private List<String> keys = List.of();
  private List<KeySpecification> keySpecs = List.of();
  private String defaultProvider;

  public StateSpecification() {
  }

  @ProtoFactory
  public StateSpecification(List<String> keys, List<KeySpecification> keySpecs, String defaultProvider) {
    this.keys = keys;
    this.keySpecs = keySpecs;
    this.defaultProvider = defaultProvider;
  }

  @ProtoField(1)
  public List<String> getKeys() {
    return keys;
  }

  @ProtoField(2)
  public List<KeySpecification> getKeySpecs() {
    return keySpecs;
  }

  @ProtoField(3)
  public String getDefaultProvider() {
    return defaultProvider;
  }
}
