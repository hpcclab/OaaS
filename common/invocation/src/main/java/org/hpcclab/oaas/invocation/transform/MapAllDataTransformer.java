package org.hpcclab.oaas.invocation.transform;

import org.hpcclab.oaas.model.object.JsonBytes;

/**
 * @author Pawissanutt
 */
public class MapAllDataTransformer implements ODataTransformer {
  @Override
  public JsonBytes transform(JsonBytes map) {
    return map;
  }

  @Override
  public JsonBytes transformMerge(JsonBytes mergeInto, JsonBytes map) {
    if (map.getNode() == null) return mergeInto;
    mergeInto.getNode().setAll(map.getNode());
    return mergeInto;
  }
}
