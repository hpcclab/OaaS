package org.hpcclab.oaas.taskmanager.service;

import io.vertx.core.json.Json;
import org.apache.kafka.common.protocol.types.Field;
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
  final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public String generateUrl(OaasObject obj,
                            String file) {
    var dac = new DataAccessContext()
      .setMainId(obj.getId())
      .setMainCls(obj.getCls());
    var b64 = genBase64Dac(dac);
    return generateUrl(obj.getId(),file, b64);
  }

  public String generateUrl(String oid,
                            String file,
                            String contextKey) {
    var saUrl = config.storageAdapterUrl();
    return saUrl + "/contents/%s/%s?contextKey=%s"
      .formatted(oid, file, contextKey);
  }

  public String generateAllocateUrl(String oid, String contextKey) {
    var saUrl = config.storageAdapterUrl();
    return saUrl + "/allocate/%s?contextKey=%s"
      .formatted(oid, contextKey);
  }

  public String genBase64Dac(DataAccessContext dac) {

    return encoder.encodeToString(Json.encode(dac).getBytes());
  }
}
