package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnativeProvision {
  @ProtoField(1)
  String image;
  @ProtoField(value = 2, defaultValue = "0")
  int minScale = 0;
  @ProtoField(value = 3, defaultValue = "-1")
  int maxScale = -1;

  @ProtoField(value = 4, defaultValue = "-1")
  int concurrency = -1;
  @ProtoField(value = 5, defaultValue = "-1")
  int targetConcurrency = -1;

  @ProtoField(value = 6)
  String scaleDownDelay = "0s";
  @ProtoField(7)
  String requestsCpu;
  @ProtoField(8)
  String requestsMemory;
  @ProtoField(9)
  String limitsCpu;
  @ProtoField(10)
  String limitsMemory;
  @ProtoField(value = 11, javaType = HashMap.class)
  Map<String, String> env;
  @ProtoField(12)
  String apiPath;

  public KnativeProvision() {
  }

  public KnativeProvision(String image,
                          int minScale,
                          int maxScale,
                          int concurrency,
                          int targetConcurrency,
                          String scaleDownDelay,
                          String requestsCpu,
                          String requestsMemory,
                          String limitsCpu,
                          String limitsMemory,
                          Map<String, String> env,
                          String apiPath) {
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
    this.env = env;
    this.apiPath = apiPath;
  }

  @ProtoFactory
  public KnativeProvision(String image,
                          int minScale,
                          int maxScale,
                          int concurrency,
                          int targetConcurrency,
                          String scaleDownDelay,
                          String requestsCpu,
                          String requestsMemory,
                          String limitsCpu,
                          String limitsMemory,
                          HashMap<String, String> env,
                          String apiPath) {
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
    this.env = env;
    this.apiPath = apiPath;
  }
}
