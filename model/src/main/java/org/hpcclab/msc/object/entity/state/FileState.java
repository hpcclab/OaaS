package org.hpcclab.msc.object.entity.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@Accessors(chain = true)
@BsonDiscriminator(key = "type", value = FileState.TYPE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileState extends MscObjectState{
  public static final String TYPE = "FILE";
  String fileUrl;
}
