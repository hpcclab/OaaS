package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.With;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@With
public record InvocationRequest(
  String invId,
  String main,
  String cls,
  String fb,
  Map<String, String> args,
  boolean immutable,
  String outId,
  JsonBytes body,
  List<InvocationChain> chains,
  @JsonIgnore String partKey
) {
  public static final String CE_TYPE = "oaas.invReq";
}
