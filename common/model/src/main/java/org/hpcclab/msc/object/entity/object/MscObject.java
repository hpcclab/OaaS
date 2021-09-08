package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.state.MscObjectState;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObject {
  @BsonId
  ObjectId id;
  MscObjectOrigin origin;
  Type type;
  Map<String, String> labels;
  //  Map<String, MscFuncMetadata> functions;
  List<String> functions = List.of();

  MscObjectState state;
  Map<String, ObjectId> members;

  public enum Type {
    RESOURCE,
    COMPOUND
  }

  public void format() {
    if (type==Type.COMPOUND) {
      state = null;
    } else {
      members = null;
    }
    if (origin==null) origin = new MscObjectOrigin().setRootId(id);
  }

  public MscObject copy() {
    return new MscObject(
      id,
      origin==null ? null:origin.copy(),
      type,
      labels==null ? null:Map.copyOf(labels),
      functions==null ? null:List.copyOf(functions),
      state,
      members==null ? null:Map.copyOf(members)
    );
  }
}
