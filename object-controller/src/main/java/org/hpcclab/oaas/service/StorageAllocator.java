package org.hpcclab.oaas.service;

import org.hpcclab.oaas.OcConfig;
import org.hpcclab.oaas.entity.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

@ApplicationScoped
public class StorageAllocator {

  @Inject
  OcConfig config;

  public void allocate(OaasObject object) {
    if (object.getId() == null) object.setId(UUID.randomUUID());
    object.getState().setBaseUrl(
      URI.create(config.s3PrefixUrl()).resolve(object.getId().toString()).toString()
    );
  }
}
