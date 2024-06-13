package org.hpcclab.oaas.invocation.transform;

import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.object.JsonBytes;

/**
 * @author Pawissanutt
 */
public interface ODataTransformer {
  JsonBytes transform(JsonBytes map);
  JsonBytes transformMerge(JsonBytes mergeInto, JsonBytes map);

  static ODataTransformer create(Dataflows.DataMapping mapping) {
    if (mapping.mapAll())
      return new MapAllDataTransformer();
    return new JaywayDataTransformer(mapping.transforms());
  }
}
