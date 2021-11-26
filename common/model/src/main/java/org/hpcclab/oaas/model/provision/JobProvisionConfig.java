package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobProvisionConfig implements Serializable {
  private String image;
  private List<String> commands;
  private List<String> containerArgs;
  private String requestsCpu;
  private String requestsMemory;
  private String limitsCpu;
  private String limitsMemory;
  private boolean argsToEnv = true;
}
