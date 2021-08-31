package org.hpcclab.msc.object.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.object.MscObject;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscFunction {
  @BsonId
  String name;
  String type;
  boolean reactive = false;
  MscObject outputTemplate;
  List<ObjectValidation> inputs;
  Task task;

  public MscFuncMetadata toMeta() {
    return new MscFuncMetadata().setName(name);
  }

}
