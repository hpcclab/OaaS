package org.hpcclab.msc.object.entity.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@Accessors(chain = true)
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value=FileState.class, name=FileState.TYPE),
  @JsonSubTypes.Type(value=RecordState.class, name=RecordState.TYPE),
  @JsonSubTypes.Type(value=StreamFilesState.class, name=StreamFilesState.TYPE),
})
@BsonDiscriminator(key = "type")
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class MscObjectState {

}
