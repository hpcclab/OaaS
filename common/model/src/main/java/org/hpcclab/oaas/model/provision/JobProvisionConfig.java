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
  String image;
  List<String> commands;
  List<String> containerArgs;
  String requestsCpu;
  String requestsMemory;
  String limitsCpu;
  String limitsMemory;
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

  @ProtoField(1)
  public String getImage() {
    return image;
  }

  @ProtoField(2)
  public List<String> getCommands() {
    return commands;
  }

  @ProtoField(3)
  public List<String> getContainerArgs() {
    return containerArgs;
  }

  @ProtoField(4)
  public String getRequestsCpu() {
    return requestsCpu;
  }

  @ProtoField(5)
  public String getRequestsMemory() {
    return requestsMemory;
  }

  @ProtoField(6)
  public String getLimitsCpu() {
    return limitsCpu;
  }

  @ProtoField(7)
  public String getLimitsMemory() {
    return limitsMemory;
  }

  @ProtoField(value = 8, defaultValue = "true")
  public boolean isArgsToEnv() {
    return argsToEnv;
  }
}
