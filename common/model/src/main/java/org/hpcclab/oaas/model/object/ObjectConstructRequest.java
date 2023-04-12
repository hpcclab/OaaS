package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.KvPair;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ObjectConstructRequest {
  String cls;
  ObjectNode embeddedRecord;
  Set<String> labels = Set.of();
  Set<String> keys = Set.of();
  Set<KvPair> overrideUrls;
  Set<ObjectReference> refs;
  List<ObjectConstructRequest> streamConstructs;

}
