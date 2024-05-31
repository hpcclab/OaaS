package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;

/**
 * @author Pawissanutt
 */
@Builder(toBuilder = true)
public record InvocationChain (
  String main,
  String cls,
  String fb,
  DSMap args,
  String invId,
  String outId,
  ObjectNode body,
  boolean immutable,
  List<InvocationChain> chains
  ){
}
