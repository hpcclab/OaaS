package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KDeploymentProvision {
  String image;
  int replicas = 1;
  String requestsCpu;
  String requestsMemory;
  String limitsCpu;
  String limitsMemory;
  String apiPath;
  int port;
  String pullPolicy;

  public KDeploymentProvision() {
  }

  public KDeploymentProvision(String image,
                              int replicas,
                              String requestsCpu,
                              String requestsMemory,
                              String limitsCpu, String limitsMemory,
                              String apiPath, int port, String pullPolicy) {
    this.image = image;
    this.replicas = replicas;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.apiPath = apiPath;
    this.port = port;
    this.pullPolicy = pullPolicy;
  }
}
