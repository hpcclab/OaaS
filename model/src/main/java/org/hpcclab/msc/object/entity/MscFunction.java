package org.hpcclab.msc.object.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.ObjectValidation;

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
  MscObject outputTemplate;
  List<ObjectValidation> inputs;
  TaskTemplate task;
  Map<String, String> macroMapping;

  public MscFuncMetadata toMeta() {
    return new MscFuncMetadata().setName(name);
  }

  public enum Type{
    TASK,
    MACRO,
    LOGICAL
  }
}
