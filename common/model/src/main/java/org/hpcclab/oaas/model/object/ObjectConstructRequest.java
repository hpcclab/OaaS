package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ObjectConstructRequest {
  String cls;
  ObjectNode embeddedRecord;
  Set<String> labels = Set.of();
  Set<String> keys = Set.of();
  Map<String, String> overrideUrls;
  Set<ObjectReference> refs;
  List<ObjectConstructRequest> streamConstructs;

}
