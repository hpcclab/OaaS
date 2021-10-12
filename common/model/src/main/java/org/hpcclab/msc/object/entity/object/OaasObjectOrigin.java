package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.persistence.Embeddable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectOrigin {
  UUID rootId;
  UUID parentId;
  String funcName;
  Map<String, String> args;
  List<UUID> additionalInputRefs;

  public OaasObjectOrigin copy() {
    return new OaasObjectOrigin(
      rootId,
      parentId,
      funcName,
      args==null ? null:Map.copyOf(args),
      additionalInputRefs==null ? null:List.copyOf(additionalInputRefs)
    );
  }

  public OaasObjectOrigin(FunctionExecContext context) {
    rootId = context.getMain().getOrigin().getRootId();
    parentId = context.getMain().getId();
    funcName = context.getFunction().getName();
    args = context.getArgs();
    additionalInputRefs = context.getAdditionalInputs()
      .stream().map(OaasObject::getId)
      .collect(Collectors.toList());
  }


  public long hash() {
    StringBuilder sb = new StringBuilder()
      .append(parentId == null? "null" :parentId.toString())
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
