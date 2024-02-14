package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KDeploymentProvision {
  @ProtoField(1)
  String image;
  @ProtoField(value = 2, defaultValue = "1")
  int replicas = 1;
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
  @ProtoField(value = 13, defaultValue = "8080")
  int port;

  public KDeploymentProvision() {
  }

  @ProtoFactory
  public KDeploymentProvision(String image, int replicas, String requestsCpu, String requestsMemory, String limitsCpu, String limitsMemory, DSMap env, String apiPath, int port) {
    this.image = image;
    this.replicas = replicas;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.env = env;
    this.apiPath = apiPath;
    this.port = port;
  }
}
