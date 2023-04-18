package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.proto.KvPair;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ObjectOrigin implements Serializable, Copyable<ObjectOrigin> {
  @ProtoField(2)
  String parentId;
  @ProtoField(3)
  String fbName;
  @ProtoField(4)
  Set<KvPair> args;
  @ProtoField(5)
  List<String> inputs = List.of();


  public ObjectOrigin() {
  }

  @ProtoFactory
  public ObjectOrigin(String parentId, String fbName, Set<KvPair> args, List<String> inputs) {
    this.parentId = parentId;
    this.fbName = fbName;
    this.args = args;
    this.inputs = inputs;
  }

  public ObjectOrigin copy() {
    return new ObjectOrigin(
      parentId,
      fbName,
      args==null ? null:Set.copyOf(args),
      inputs==null ? null:List.copyOf(inputs)
    );
  }


  public long hash() {
    StringBuilder sb = new StringBuilder()
      .append(parentId == null? "null" :parentId)
      .append(fbName);
    if (args != null && !args.isEmpty()) {
      args.stream()
        .sorted(Comparator.comparing(KvPair::getKey))
        .forEach(e -> sb.append(e.getKey())
          .append(e.getVal())
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
