package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectRequirement implements Serializable{
  Set<String> requiredLabels = Set.of();
  String requiredClass;

  public OaasObjectRequirement() {
  }

  @ProtoFactory
  public OaasObjectRequirement(Set<String> requiredLabels, String requiredClass) {
    this.requiredLabels = requiredLabels;
    this.requiredClass = requiredClass;
  }

  @ProtoField(1)
  public Set<String> getRequiredLabels() {
    return requiredLabels;
  }

  @ProtoField(2)
  public String getRequiredClass() {
    return requiredClass;
  }
}
