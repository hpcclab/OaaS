package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ObjectOrigin implements Serializable {
//  String rootId;
  @ProtoField(2)
  String parentId;
  @ProtoField(3)
  String funcName;
  @ProtoField(number = 4, javaType = HashMap.class)
  Map<String, String> args;
  @ProtoField(5)
  List<String> inputs = List.of();

  @ProtoField(value = 6, defaultValue = "true")
  boolean wfi = true;

  public ObjectOrigin() {
  }

  public ObjectOrigin(String parentId, String funcName, Map<String, String> args, List<String> inputs, boolean wfi) {
    this.parentId = parentId;
    this.funcName = funcName;
    this.args = args;
    this.inputs = inputs;
    this.wfi = wfi;
  }

  @ProtoFactory
  public ObjectOrigin(String parentId, String funcName, HashMap<String, String> args, List<String> inputs, boolean wfi) {
    this.parentId = parentId;
    this.funcName = funcName;
    this.args = args;
    this.inputs = inputs;
    this.wfi = wfi;
  }

  public ObjectOrigin copy() {
    return new ObjectOrigin(
      parentId,
      funcName,
      args==null ? null:Map.copyOf(args),
      inputs==null ? null:List.copyOf(inputs),
      wfi
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
    if (inputs!= null) {
      inputs.forEach(sb::append);
    }
    var crc = new PureJavaCrc32();
      crc.update(sb.toString().getBytes(StandardCharsets.UTF_8));
    return crc.hashCode();
  }

  @JsonIgnore
  public boolean isRoot() {
    return parentId == null;
  }
}
