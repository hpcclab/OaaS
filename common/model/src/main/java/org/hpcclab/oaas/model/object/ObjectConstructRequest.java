package org.hpcclab.oaas.model.object;

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
  Set<ObjectReference> refs;
  List<ObjectConstructRequest> streamConstructs;

}
