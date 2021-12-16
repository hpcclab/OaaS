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

  public StateSpecification() {
  }

  @ProtoFactory
  public StateSpecification(List<String> keys) {
    this.keys = keys;
  }

  @ProtoField(1)
  public List<String> getKeys() {
    return keys;
  }
}
