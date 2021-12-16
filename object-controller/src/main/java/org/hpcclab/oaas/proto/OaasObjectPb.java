package org.hpcclab.oaas.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hpcclab.oaas.model.function.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.object.OaasCompoundMemberDto;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectPb {
  @ProtoField(1)
  UUID id;
  @ProtoField(2)
  OaasObjectOrigin origin;
//  @ProtoField(3)
//  Long originHash;
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

}
