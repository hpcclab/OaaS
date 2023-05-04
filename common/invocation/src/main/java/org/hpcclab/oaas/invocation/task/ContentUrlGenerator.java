package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.Map;
import java.util.stream.Collectors;

public interface ContentUrlGenerator {
  String generateUrl(OaasObject obj,
                     String file,
                     AccessLevel level);

  String generateUrl(OaasObject obj,
                     DataAccessContext dac,
                     String file);

  default Map<String, String> generateUrl(OaasObject obj,
                                          AccessLevel level) {
    return obj.getState().getVerIds()
      .stream()
      .map(kv -> {
        var url = generateUrl(obj, kv.getKey(), level);
        return Map.entry(kv.getKey(), url);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  String generateAllocateUrl(OaasObject obj, DataAccessContext dac);
}
