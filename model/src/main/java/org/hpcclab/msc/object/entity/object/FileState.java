package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@Accessors(chain = true)
@BsonDiscriminator(key = "type", value = "FILE")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileState extends MscObjectState{
  String fileUrl;
}
