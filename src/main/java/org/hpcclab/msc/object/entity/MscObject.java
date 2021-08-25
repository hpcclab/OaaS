package org.hpcclab.msc.object.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObject {
  @BsonId
  ObjectId id;
  MscObjectOrigin origin;
  String type;
  Map<String, MscFuncMetadata> functions;
  MscObjectState state;
}
