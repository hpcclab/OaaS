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

  @ProtoField(value = 5)
  String scaleDownDelay = "0s";
  @ProtoField(6)
  String requestsCpu;
  @ProtoField(7)
  String requestsMemory;
  @ProtoField(8)
  String limitsCpu;
  @ProtoField(9)
  String limitsMemory;
  @ProtoField(value = 10, javaType = HashMap.class)
  Map<String, String> env;

  public KnativeProvision() {
  }

  public KnativeProvision(String image,
                          int minScale,
                          int maxScale,
                          int concurrency,
                          String scaleDownDelay,
                          String requestsCpu,
                          String requestsMemory,
                          String limitsCpu,
                          String limitsMemory,
                          Map<String, String> env) {
    this.image = image;
    this.minScale = minScale;
    this.maxScale = maxScale;
    this.concurrency = concurrency;
    this.scaleDownDelay = scaleDownDelay;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.env = env;
  }

  @ProtoFactory
  public KnativeProvision(String image,
                          int minScale,
                          int maxScale,
                          int concurrency,
                          String scaleDownDelay,
                          String requestsCpu,
                          String requestsMemory,
                          String limitsCpu,
                          String limitsMemory,
                          HashMap<String, String> env) {
    this.image = image;
    this.minScale = minScale;
    this.maxScale = maxScale;
    this.concurrency = concurrency;
    this.scaleDownDelay = scaleDownDelay;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.env = env;
  }
}
