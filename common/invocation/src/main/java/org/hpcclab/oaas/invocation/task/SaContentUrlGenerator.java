package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.IOObject;
import org.hpcclab.oaas.model.object.OObject;

public class SaContentUrlGenerator implements ContentUrlGenerator {

  private final String saUrl;

  public SaContentUrlGenerator(String saUrl) {
    this.saUrl = saUrl;
  }

  public String generateUrl(IOObject<?> obj,
                            DataAccessContext dac,
                            String file) {
    return generateUrl(obj.getKey(), dac.getVid(), file, dac.encode());
  }

  @Override
  public String generatePutUrl(IOObject<?> obj, DataAccessContext dac, String file) {
    throw new UnsupportedOperationException("Not supported.");
  }

  private String generateUrl(String oid,
                             String vid,
                             String file,
                             String contextKey) {
    return saUrl + "/contents/%s/%s/%s?contextKey=%s"
      .formatted(oid, vid, file, contextKey);
  }

  public String generateAllocateUrl(IOObject<?> obj, DataAccessContext dac) {
    return saUrl + "/allocate/%s?contextKey=%s"
      .formatted(obj.getKey(), dac.encode());
  }
}
