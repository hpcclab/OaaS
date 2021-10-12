package org.hpcclab.msc.object.service;

import org.bson.types.ObjectId;
import org.hpcclab.msc.object.OcConfig;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.file.Path;
import java.util.UUID;

@ApplicationScoped
public class StorageAllocator {

  @Inject
  OcConfig config;

  public void allocate(OaasObject object) {
    if (object.getId() == null) object.setId(UUID.randomUUID());
    object.getState().setBaseUrl(
      Path.of(config.s3PrefixUrl()).resolve(object.getId().toString()).toString()
    );
  }
}
