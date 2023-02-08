package org.hpcclab.oaas.invocation;

import org.hpcclab.oaas.invocation.config.InvocationConfig;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ContentUrlGenerator {

  private final String saUrl;

  @Inject
  public ContentUrlGenerator(InvocationConfig config) {
    this.saUrl = config.storageAdapterUrl();
  }

  public ContentUrlGenerator(String saUrl) {
    this.saUrl = saUrl;
  }

  public String generateUrl(OaasObject obj,
                            String file,
                            AccessLevel level) {
    if (obj.getState().getVerIds() == null || obj.getState().getVerIds().isEmpty())
      throw StdOaasException.notKeyInObj(obj.getId(),404);
    var vid = obj.getState().getVerIds().get(file);
    if (vid == null)
      throw StdOaasException.notKeyInObj(obj.getId(),404);
    var dac = DataAccessContext.generate(obj, level)
      .setVid(vid);
    var b64 = dac.encode();
    return generateUrl(obj.getId(), vid, file, b64);
  }

  public String generateUrl(String oid,
                            String vid,
                            String file,
                            String contextKey) {
    return saUrl + "/contents/%s/%s/%s?contextKey=%s"
      .formatted(oid, vid, file, contextKey);
  }

  public String generateAllocateUrl(String oid, String contextKey) {
    return saUrl + "/allocate/%s?contextKey=%s"
      .formatted(oid, contextKey);
  }
}
