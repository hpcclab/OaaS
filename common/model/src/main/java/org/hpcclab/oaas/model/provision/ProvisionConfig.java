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
public class ProvisionConfig implements Serializable {
  @ProtoField(1)
  JobProvisionConfig job;
  @ProtoField(2)
  KnativeProvision knative;
  @ProtoField(3)
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
  public ProvisionConfig(JobProvisionConfig job, KnativeProvision knative, Type type) {
    this.job = job;
    this.knative = knative;
    this.type = type;
  }

  public void validate() {
    if (job == null && knative == null)
      throw new OaasValidationException("Provision config must be defined only one type. (No definition found)");
    if (job != null && knative != null)
      throw new OaasValidationException("Provision config must be defined only one type.");
    if (job != null) type = Type.EPHEMERAL;
    if (knative != null) type = Type.DURABLE;
  }
}
