package org.hpcclab.msc.object.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.hpcclab.msc.object.entity.object.MscObjectRequirement;
import org.hpcclab.msc.object.entity.object.MscObjectTemplate;
import org.hpcclab.msc.object.model.SubFunctionCall;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscFunction {
  @BsonId
  String name;
  Type type;
  boolean reactive = false;
  MscObjectTemplate outputTemplate;
  MscObjectRequirement bindingRequirement;
  List<MscObjectRequirement> additionalInputs = List.of();
  TaskTemplate task;
  // memberToFunction
//  Map<String, String> macroMapping;
  Map<String, SubFunctionCall> subFunctions;

  public MscFuncMetadata toMeta() {
    return new MscFuncMetadata().setName(name);
  }

  public enum Type{
    TASK,
    MACRO,
    LOGICAL
  }
}
