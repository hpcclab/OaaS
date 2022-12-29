package org.hpcclab.oaas.invocation;

import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ContentUrlGenerator {

  private final String saUrl;

  @Inject
  public ContentUrlGenerator(InvocationConfig config) {
    this.saUrl = config.storageAdapterUrl;
  }

  public ContentUrlGenerator(String saUrl) {
    this.saUrl = saUrl;
  }

  public String generateUrl(OaasObject obj,
                            String file) {
    var dac = DataAccessContext.generate(obj);
    var b64 = dac.encode();
    return generateUrl(obj.getId(), file, b64);
  }

  public String generateUrl(String oid,
                            String file,
                            String contextKey) {
    return saUrl + "/contents/%s/%s?contextKey=%s"
      .formatted(oid, file, contextKey);
  }

  public String generateAllocateUrl(String oid, String contextKey) {
    return saUrl + "/allocate/%s?contextKey=%s"
      .formatted(oid, contextKey);
  }
}
