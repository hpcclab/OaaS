package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
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
  @ProtoField(value = 11)
  DSMap env;
  @ProtoField(12)
  String apiPath;
  @ProtoField(value = 13, defaultValue = "-1")
  int port;

  public KnativeProvision() {
  }

  @ProtoFactory
  public KnativeProvision(String image, int minScale, int maxScale, int concurrency, int targetConcurrency, String scaleDownDelay, String requestsCpu, String requestsMemory, String limitsCpu, String limitsMemory, DSMap env, String apiPath, int port) {
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
    this.port = port;
  }
}
