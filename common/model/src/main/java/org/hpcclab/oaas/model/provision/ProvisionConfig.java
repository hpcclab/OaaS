package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.OaasValidationException;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProvisionConfig {
  private JobProvisionConfig job;
  private KnativeProvision knative;
  private Type type;
  public enum Type {
    EPHEMERAL, DURABLE
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
