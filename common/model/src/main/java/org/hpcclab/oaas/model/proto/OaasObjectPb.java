package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
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
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;
  String cls;
  Set<String> labels;
//  @ProtoField(7)
//  Set<OaasFunctionBindingDto> functions = Set.of();
  OaasObjectState state;
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

  @ProtoField(1)
  public UUID getId() {
    return id;
  }

  @ProtoField(2)
  public OaasObjectOrigin getOrigin() {
    return origin;
  }

  @ProtoField(3)
  public Long getOriginHash() {
    return originHash;
  }

  @ProtoField(4)
  public ObjectAccessModifier getAccess() {
    return access;
  }

  @ProtoField(5)
  public String getCls() {
    return cls;
  }

  @ProtoField(6)
  public Set<String> getLabels() {
    return labels;
  }

  @ProtoField(8)
  public OaasObjectState getState() {
    return state;
  }

  @ProtoField(9)
  public Set<OaasCompoundMemberDto> getMembers() {
    return members;
  }
}
