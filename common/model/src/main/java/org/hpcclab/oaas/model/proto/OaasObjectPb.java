package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hpcclab.oaas.model.object.OaasCompoundMemberDto;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectPb {
  @ProtoField(1)
  UUID id;
  @ProtoField(2)
  OaasObjectOrigin origin;
  @ProtoField(3)
  Long originHash;
  @ProtoField(4)
  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;
  @ProtoField(5)
  String cls;
  @ProtoField(6)
  Set<String> labels;
//  @ProtoField(7)
//  Set<OaasFunctionBindingDto> functions = Set.of();
  @ProtoField(8)
  OaasObjectState state;
  @ProtoField(9)
  Set<OaasCompoundMemberDto> members;

  public OaasObjectPb() {
  }

  @ProtoFactory
  public OaasObjectPb(UUID id, OaasObjectOrigin origin, Long originHash, ObjectAccessModifier access, String cls, Set<String> labels, OaasObjectState state, Set<OaasCompoundMemberDto> members) {
    this.id = id;
    this.origin = origin;
    this.originHash = originHash;
    this.access = access;
    this.cls = cls;
    this.labels = labels;
    this.state = state;
    this.members = members;
  }

  public static OaasObjectPb createFromClasses(OaasClassPb cls) {
    var o = new OaasObjectPb();
    o.setCls(cls.getName());
    o.setState(new OaasObjectState().setType(cls.getStateType()));
    return o;
  }

  public Optional<OaasCompoundMemberDto> findMember(String name) {
    return members.stream()
      .filter(mem -> mem.getName().equals(name))
      .findFirst();
  }
  public OaasObjectPb copy() {
    return new OaasObjectPb(
      id,
      origin==null ? null:origin.copy(),
      originHash,
      access,
      cls,
      labels==null ? null:Set.copyOf(labels),
      state,
      members==null ? null:Set.copyOf(members)
    );
  }
}
