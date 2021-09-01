package org.hpcclab.msc.object.entity.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@BsonDiscriminator(key = "type", value = StreamFilesState.TYPE)
public class StreamFilesState extends MscObjectState{
  public static final String TYPE = "STREAM_FILES";
  String fileUrl;
  String groupId;
}
