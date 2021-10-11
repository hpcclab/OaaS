package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObject {
  @BsonId
  ObjectId id;
  OaasObjectOrigin origin;
  long originHash;
  Type type;
  AccessModifier access;
  Map<String, String> labels;
  List<String> functions = List.of();

  OaasObjectState state;
  Map<String, ObjectId> members;

  public enum Type {
    RESOURCE,
    COMPOUND
  }

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    FINAL
  }

  public void format() {
    if (type==Type.COMPOUND) {
      state = null;
    } else {
      members = null;
    }
    if (origin==null) origin =new OaasObjectOrigin().setRootId(id);
  }

  public OaasObject copy() {
    return new OaasObject(
      id,
      origin==null ? null:origin.copy(),
      originHash,
      type,
      access,
      labels==null ? null:Map.copyOf(labels),
      functions==null ? null:List.copyOf(functions),
      state,
      members==null ? null:Map.copyOf(members)
    );
  }

  public void updateHash() {
    this.originHash = origin.hash();
  }
}
