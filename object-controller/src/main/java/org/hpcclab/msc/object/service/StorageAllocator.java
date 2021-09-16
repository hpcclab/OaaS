package org.hpcclab.msc.object.service;

import org.hpcclab.msc.object.OcConfig;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class StorageAllocator {

  @Inject
  OcConfig config;

  public void allocate(MscObject object) {
    object.getState().setUrl(config.s3PrefixUrl() + "/" + object.getId().toHexString());
  }
}
