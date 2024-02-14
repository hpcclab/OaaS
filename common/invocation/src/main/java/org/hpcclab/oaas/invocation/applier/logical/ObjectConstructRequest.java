package org.hpcclab.oaas.invocation.applier.logical;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ObjectConstructRequest {
  String cls;
  ObjectNode data;
  Set<String> keys = Set.of();
  DSMap overrideUrls;
  DSMap refs;
  List<ObjectConstructRequest> streamConstructs;

}