package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateSpecification implements Copyable<StateSpecification> {
  List<KeySpecification> keySpecs = List.of();
  String defaultProvider;

  public StateSpecification() {
  }

  @ProtoFactory
  public StateSpecification(List<KeySpecification> keySpecs,
                            String defaultProvider) {
    this.keySpecs = keySpecs;
    this.defaultProvider = defaultProvider;
  }

  public void validate() {
    if (defaultProvider == null) defaultProvider = "s3";
  }


  @ProtoField(1)
  public List<KeySpecification> getKeySpecs() {
    return keySpecs;
  }

  @ProtoField(2)
  public String getDefaultProvider() {
    return defaultProvider;
  }

  @Override
  public StateSpecification copy() {
    return new StateSpecification(
      keySpecs,
      defaultProvider
    );
  }
}
