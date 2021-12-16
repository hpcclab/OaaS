package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobProvisionConfig implements Serializable {
  @ProtoField(1)
  String image;
  @ProtoField(2)
  List<String> commands;
  @ProtoField(3)
  List<String> containerArgs;
  @ProtoField(4)
  String requestsCpu;
  @ProtoField(5)
  String requestsMemory;
  @ProtoField(6)
  String limitsCpu;
  @ProtoField(7)
  String limitsMemory;
  @ProtoField(value = 8, defaultValue = "true")
  boolean argsToEnv = true;

  public JobProvisionConfig() {
  }

  @ProtoFactory
  public JobProvisionConfig(String image, List<String> commands, List<String> containerArgs, String requestsCpu, String requestsMemory, String limitsCpu, String limitsMemory, boolean argsToEnv) {
    this.image = image;
    this.commands = commands;
    this.containerArgs = containerArgs;
    this.requestsCpu = requestsCpu;
    this.requestsMemory = requestsMemory;
    this.limitsCpu = limitsCpu;
    this.limitsMemory = limitsMemory;
    this.argsToEnv = argsToEnv;
  }
}
