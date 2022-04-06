package org.hpcclab.oaas.model.object;

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
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectOrigin implements Serializable {
  String rootId;
  String parentId;
  String funcName;
  Map<String, String> args;
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  List<String> inputs = List.of();

  public OaasObjectOrigin() {
  }

  public OaasObjectOrigin(String rootId, String parentId, String funcName, Map<String, String> args, List<String> inputs) {
    this.rootId = rootId;
    this.parentId = parentId;
    this.funcName = funcName;
    this.args = args;
    this.inputs = inputs;
  }

  @ProtoFactory
  public OaasObjectOrigin(String rootId, String parentId, String funcName, HashMap<String, String> args, List<String> inputs) {
    this.rootId = rootId;
    this.parentId = parentId;
    this.funcName = funcName;
    this.args = args;
    this.inputs = inputs;
  }

  public OaasObjectOrigin copy() {
    return new OaasObjectOrigin(
      rootId,
      parentId,
      funcName,
      args==null ? null:Map.copyOf(args),
      inputs==null ? null:List.copyOf(inputs)
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

  @ProtoField(1)
  public String getRootId() {
    return rootId;
  }

  @ProtoField(2)
  public String getParentId() {
    return parentId;
  }

  @ProtoField(3)
  public String getFuncName() {
    return funcName;
  }

  @ProtoField(number = 4, javaType = HashMap.class)
  public Map<String, String> getArgs() {
    return args;
  }

  @ProtoField(5)
  public List<String> getInputs() {
    return inputs;
  }
}
