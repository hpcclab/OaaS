package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.IOObject;

import java.util.Map;

public interface ContentUrlGenerator {
  default String generateUrl(IOObject<?> obj,
                             String file,
                             AccessLevel level,
                             boolean usePublic) {
    Map<String, String> verIds = obj.getMeta().getVerIds();
    if (verIds==null || verIds.isEmpty())
      throw StdOaasException.notKeyInObj(obj.getKey(), 404);
    var vid = obj.getMeta().getVerIds().get(file);
    if (vid==null)
      throw StdOaasException.notKeyInObj(obj.getKey(), 404);
    var dac = DataAccessContext.generate(obj, level, vid, usePublic);
    return generateUrl(obj, dac, file);
  }

  default String generatePutUrl(IOObject<?> obj,
                                String file,
                                AccessLevel level,
                                boolean usePublic) {
    Map<String, String> verIds = obj.getMeta().getVerIds();
    if (verIds==null || verIds.isEmpty())
      throw StdOaasException.notKeyInObj(obj.getKey(), 404);
    var vid = obj.getMeta().getVerIds().get(file);
    if (vid==null)
      throw StdOaasException.notKeyInObj(obj.getKey(), 404);
    var dac = DataAccessContext.generate(obj, level, vid, usePublic);
    return generatePutUrl(obj, dac, file);
  }

  String generateUrl(IOObject<?> obj,
                     DataAccessContext dac,
                     String file);

  String generatePutUrl(IOObject<?> obj,
                        DataAccessContext dac,
                        String file);


  String generateAllocateUrl(IOObject<?> obj, DataAccessContext dac);
}
