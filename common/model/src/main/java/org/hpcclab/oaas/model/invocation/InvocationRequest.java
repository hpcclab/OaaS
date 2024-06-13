package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.With;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@With
public record InvocationRequest(
  String main,
  String cls,
  String fb,
  DSMap args,
  boolean immutable,
  String invId,
  String outId,
  JsonBytes body,
  List<InvocationChain> chains,
  @JsonIgnore String partKey
) {
  public static final String CE_TYPE = "oaas.invReq";
}
