package org.hpcclab.oaas.invocation.task;

import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.OaasObject;

public class SaContentUrlGenerator implements ContentUrlGenerator {

  private final String saUrl;

//  public SaContentUrlGenerator() {
//    saUrl = "";
//  }

  public SaContentUrlGenerator(String saUrl) {
    this.saUrl = saUrl;
  }

  public String generateUrl(OaasObject obj,
                            DataAccessContext dac,
                            String file) {
    return generateUrl(obj.getId(), dac.getVid(), file, dac.encode());
  }

  private String generateUrl(String oid,
                             String vid,
                             String file,
                             String contextKey) {
    return saUrl + "/contents/%s/%s/%s?contextKey=%s"
      .formatted(oid, vid, file, contextKey);
  }

  public String generateAllocateUrl(OaasObject obj, DataAccessContext dac) {
    return saUrl + "/allocate/%s?contextKey=%s"
      .formatted(obj.getId(), dac.encode());
  }
}
