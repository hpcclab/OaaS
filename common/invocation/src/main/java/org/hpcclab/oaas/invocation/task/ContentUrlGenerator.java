package org.hpcclab.oaas.invocation.task;

import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.OObject;

import java.util.Map;

public interface ContentUrlGenerator {
  default String generateUrl(OObject obj,
                             String file,
                             AccessLevel level,
                             boolean usePublic) {
    if (obj.getState().getVerIds()==null || obj.getState().getVerIds().isEmpty())
      throw StdOaasException.notKeyInObj(obj.getId(), 404);
    var vid = obj.getState().findVerId(file);
    if (vid==null)
      throw StdOaasException.notKeyInObj(obj.getId(), 404);
    var dac = DataAccessContext.generate(obj, level, vid, usePublic);
    return generateUrl(obj, dac, file);
  }
  default Map<String, String> generateUrl(OObject obj,
                                          AccessLevel level) {
    return obj.getState().getVerIds()
      .collect((k, v) -> Tuples.pair(k, generateUrl(obj, k, level, false)));
  }


  String generateUrl(OObject obj,
                     DataAccessContext dac,
                     String file);


  String generateAllocateUrl(OObject obj, DataAccessContext dac);
}
