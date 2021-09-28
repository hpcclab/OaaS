package org.hpcclab.msc.object.service;

import org.bson.types.ObjectId;
import org.hpcclab.msc.object.OcConfig;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.file.Path;

@ApplicationScoped
public class StorageAllocator {

  @Inject
  OcConfig config;

  public void allocate(MscObject object) {
    if (object.getId() == null) object.setId(new ObjectId());
    object.getState().setBaseUrl(
      Path.of(config.s3PrefixUrl()).resolve(object.getId().toHexString()).toString()
    );
  }
}
