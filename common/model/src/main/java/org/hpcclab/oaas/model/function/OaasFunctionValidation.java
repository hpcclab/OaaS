package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectRequirement;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionValidation implements Serializable {
  private OaasObjectRequirement bindingRequirement;
  private List<OaasObjectRequirement> additionalInputs = List.of();
}
