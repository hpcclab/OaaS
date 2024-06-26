package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KnativeProvision {
  String image;
  int minScale = -1;
  int maxScale = -1;
  int concurrency = -1;
  int targetConcurrency = -1;
  String scaleDownDelay = "0s";
  String requestsCpu;
  String requestsMemory;
  String limitsCpu;
  String limitsMemory;
  String apiPath;
  int port;

  public KnativeProvision() {
  }

  public KnativeProvision(String image, int minScale, int maxScale,
                          int concurrency, int targetConcurrency, String scaleDownDelay,
                          String requestsCpu, String requestsMemory, String limitsCpu,
                          String limitsMemory, String apiPath, int port) {
    this.image = image;
    this.minScale = minScale;
    this.maxScale = maxScale;
    this.concurrency = concurrency;
    this.targetConcurrency = targetConcurrency;
    this.scaleDownDelay = scaleDownDelay;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.apiPath = apiPath;
    this.port = port;
  }

}
