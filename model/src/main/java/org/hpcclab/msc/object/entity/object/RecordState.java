package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;

import java.util.Map;


@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@BsonDiscriminator(key = "type", value = RecordState.TYPE)
public class RecordState extends MscObjectState{
  public static final String TYPE = "RECORD";
  Map<String, String> records;
}
