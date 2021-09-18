package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.model.FunctionExecContext;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MscObjectOrigin {
  ObjectId rootId;
  ObjectId parentId;
  String funcName;
  Map<String, String> args;
  List<ObjectId> additionalInputRefs;

  public MscObjectOrigin copy() {
    return new MscObjectOrigin(
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


  public long hash() {
    StringBuilder sb = new StringBuilder()
      .append(parentId.toHexString())
      .append(funcName);
    if (args != null && !args.isEmpty()) {
      args.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> sb.append(e.getKey())
          .append(e.getValue())
        );
    }
    if (additionalInputRefs!= null) {
      additionalInputRefs.forEach(sb::append);
    }
    var crc = new PureJavaCrc32();
      crc.update(sb.toString().getBytes(StandardCharsets.UTF_8));
    return crc.hashCode();
  }


}
