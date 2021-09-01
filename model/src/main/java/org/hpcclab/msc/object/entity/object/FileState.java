package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@Accessors(chain = true)
@BsonDiscriminator(key = "type", value = FileState.TYPE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileState extends MscObjectState{
  public static final String TYPE = "RECORD";
  String fileUrl;
}
