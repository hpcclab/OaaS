package org.hpcclab.msc.object.entity.function;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.hpcclab.msc.object.entity.object.OaasObjectRequirement;
import org.hpcclab.msc.object.entity.object.OaasObjectTemplate;
import org.hpcclab.msc.object.entity.task.TaskConfiguration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunction {
  @BsonId
  @NotBlank
  String name;
  @NotNull
  Type type;
  boolean reactive = false;
  OaasObjectTemplate outputTemplate;
  OaasObjectRequirement bindingRequirement;
  List<OaasObjectRequirement> additionalInputs = List.of();
  TaskConfiguration task;
  Map<String, SubFunctionCall> subFunctions;


  public enum Type {
    TASK,
    MACRO,
    LOGICAL
  }
}
