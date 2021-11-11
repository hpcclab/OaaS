package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.PureJavaCrc32;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<UUID> additionalInputs = List.of();

  public OaasObjectOrigin copy() {
    return new OaasObjectOrigin(
      rootId,
      parentId,
      funcName,
      args==null ? null:Map.copyOf(args),
      additionalInputs==null ? null:List.copyOf(additionalInputs)
    );
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
    if (additionalInputs!= null) {
      additionalInputs.forEach(sb::append);
    }
    var crc = new PureJavaCrc32();
      crc.update(sb.toString().getBytes(StandardCharsets.UTF_8));
    return crc.hashCode();
  }


}
