package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.IOObject;

public class DefaultContentUrlGenerator implements ContentUrlGenerator {

  private final String prefixUrl;

  public DefaultContentUrlGenerator(String saUrl) {
    this.prefixUrl = saUrl;
  }

  public String generateUrl(IOObject<?> obj,
                            DataAccessContext dac,
                            String file) {
    return prefixUrl + "/contents/%s/%s/%s?contextKey=%s"
      .formatted(obj.getKey(), dac.getVid(), file, dac.encode());
  }

  @Override
  public String generatePutUrl(IOObject<?> obj, DataAccessContext dac, String file) {
    throw new UnsupportedOperationException("Not supported.");
  }

  public String generateAllocateUrl(IOObject<?> obj, DataAccessContext dac) {
    return prefixUrl + "/allocate/%s?contextKey=%s"
      .formatted(obj.getKey(), dac.encode());
  }
}
