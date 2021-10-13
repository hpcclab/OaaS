package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Embeddable
public class OaasObjectRequirement implements Serializable{
//  @ElementCollection
//  Map<String, String> requiredLabel;
  OaasObject.ObjectType requiredType;
  String requiredStateType;
}
