package org.hpcclab.msc.object.entity;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscFunction {
  @BsonId
  ObjectId id;
  String name;
  String type;
  boolean splittable = true;

  public MscFuncMetadata toMeta() {
    return new MscFuncMetadata().setName(name);
  }
}
