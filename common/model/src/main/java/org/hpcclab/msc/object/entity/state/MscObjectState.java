package org.hpcclab.msc.object.entity.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonDiscriminator;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObjectState {
  Type type;
  String baseUrl;
  String file;
  List<String> files;
  Map<String, String> records;
  String groupId;

  public enum Type {
    FILE, FILES, SEGMENTABLE, RECORD
  }
}
