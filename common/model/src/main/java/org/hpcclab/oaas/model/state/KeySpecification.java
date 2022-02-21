package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeySpecification {
  String name;
  String provider;

  public KeySpecification() {
  }

  @ProtoFactory
  public KeySpecification(String name, String provider) {
    this.name = name;
    this.provider = provider;
  }

  @ProtoField(1)
  public String getName() {
    return name;
  }

  @ProtoField(2)
  public String getProvider() {
    return provider;
  }
}
