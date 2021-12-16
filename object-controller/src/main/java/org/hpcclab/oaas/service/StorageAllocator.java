package org.hpcclab.oaas.service;

import org.hpcclab.oaas.OcConfig;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.util.UUID;

@ApplicationScoped
public class  StorageAllocator {
  private static final Logger LOGGER = LoggerFactory.getLogger( StorageAllocator.class );

  @Inject
  OcConfig config;

  public void allocate(OaasObjectPb object) {
    if (object.getId() == null) object.setId(UUID.randomUUID());
    var prefix = config.s3PrefixUrl();
    if (config.s3PrefixUrl().charAt(config.s3PrefixUrl().length() - 1) != '/') {
      prefix += '/';
    }
    object.getState().setBaseUrl(
      URI.create(prefix).resolve(object.getId().toString()).toString()
    );
  }
}
