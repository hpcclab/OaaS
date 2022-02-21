package org.hpcclab.oaas.taskmanager.service;

import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class ContentUrlGenerator {
  @Inject
  TaskManagerConfig config;

  public String generateUrl(OaasObject obj,
                            String file) {
    var dac = new DataAccessContext()
      .setMainId(obj.getId())
      .setMainCls(obj.getCls());
    var b64 = genBase64Dac(dac);
    return generateUrl(obj.getId(),file, b64);
  }

  public String generateUrl(UUID oid,
                            String file,
                            String contextKey) {
    var saUrl = config.storageAdapterUrl();
    return saUrl + "/contents/%s/%s?contextKey=%s"
      .formatted(oid, file, contextKey);
  }

  public String generateAllocateUrl(UUID oid, String contextKey) {
    var saUrl = config.storageAdapterUrl();
    return saUrl + "/contents/%s?contextKey=%s"
      .formatted(oid, contextKey);
  }

  public String genBase64Dac(DataAccessContext dac) {

    return Base64.getUrlEncoder().encodeToString(Json.encode(dac).getBytes());
  }
}
