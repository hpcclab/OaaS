package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscFunction;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObjectOrigin {
  Boolean root;
  ObjectId rootId;
  ObjectId parentId;
  String funcName;
  Map<String, String> args;

  public MscObjectOrigin copy() {
    return new MscObjectOrigin(root, rootId, parentId, funcName, args==null ? null:Map.copyOf(args));
  }

  public MscObjectOrigin(MscObject object,
                         MscFunction function,
                         Map<String, String> args) {
    rootId = object.getOrigin().getRootId();
    parentId = object.getId();
    funcName = function.getName();
    this.args = args;
  }
}
