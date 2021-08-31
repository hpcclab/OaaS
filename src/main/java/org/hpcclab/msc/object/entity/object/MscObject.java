package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscFuncMetadata;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObject {
  @BsonId
  ObjectId id;
  MscObjectOrigin origin;
  Type type;
  Map<String, MscFuncMetadata> functions;
  MscObjectState state;

  public enum Type{
    RESOURCE,
    COMPOUND
  }
}
