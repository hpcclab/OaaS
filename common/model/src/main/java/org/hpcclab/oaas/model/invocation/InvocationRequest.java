package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.With;
import org.hpcclab.oaas.model.object.OObject;
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
  List<String> inputs,
  List<OObject> inputObjects,
  boolean immutable,
  boolean macro,
  String invId,
  String outId,
  DSMap macroIds,
  boolean preloadingNode,
  long queTs,
  ObjectNode body,
  @JsonIgnore String partKey
) {
  public static final String CE_TYPE = "oaas.invReq";
}
