package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.model.FunctionExecContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  List<ObjectId> additionalInputRefs;

  public MscObjectOrigin copy() {
    return new MscObjectOrigin(
      root,
      rootId,
      parentId,
      funcName,
      args==null ? null:Map.copyOf(args),
      additionalInputRefs==null ? null:List.copyOf(additionalInputRefs)
    );
  }

  public MscObjectOrigin(FunctionExecContext context) {
    rootId = context.getTarget().getOrigin().getRootId();
    parentId = context.getTarget().getId();
    funcName = context.getFunction().getName();
    args = context.getArgs();
    additionalInputRefs = context.getAdditionalInputs()
      .stream().map(MscObject::getId)
      .collect(Collectors.toList());
  }
}
