package org.hpcclab.oaas.invocation.transform;

import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface ODataTransformer {
  static ODataTransformer create(Dataflows.DataMapping mapping) {
    if (mapping.mapAll())
      return new MapAllDataTransformer();
    return new JaywayDataTransformer(mapping.transforms());
  }

  static ODataTransformer create(List<Dataflows.Transformation> transformations) {
    if (transformations==null || transformations.isEmpty())
      return new MapAllDataTransformer();
    return new JaywayDataTransformer(transformations);
  }

  JsonBytes transform(JsonBytes map);

  JsonBytes transformMerge(JsonBytes mergeInto, JsonBytes map);
}
