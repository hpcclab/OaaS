package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProvisionConfig {
  KnativeProvision knative;
  Type type;

  public enum Type {
    @ProtoEnumValue(1)
    EPHEMERAL,
    @ProtoEnumValue(2)
    DURABLE
  }

  public ProvisionConfig() {
  }

  @ProtoFactory
  public ProvisionConfig(KnativeProvision knative, Type type) {
    this.knative = knative;
    this.type = type;
  }

  public void validate() {
    if (knative != null) type = Type.DURABLE;
  }

  @ProtoField(2)
  public KnativeProvision getKnative() {
    return knative;
  }

  @ProtoField(3)
  public Type getType() {
    return type;
  }
}
