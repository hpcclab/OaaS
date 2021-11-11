package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectRequirement;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionValidation {
  private OaasObjectRequirement bindingRequirement;
  private List<OaasObjectRequirement> additionalInputs = List.of();
}
