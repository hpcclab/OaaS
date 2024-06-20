package org.hpcclab.oaas.model.invocation;

import lombok.Builder;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
public record InvocationChain (
  String invId,
  String main,
  String cls,
  String fb,
  Map<String, String> args,
  String outId,
  JsonBytes body,
  boolean immutable,
  List<InvocationChain> chains
  ){
}
