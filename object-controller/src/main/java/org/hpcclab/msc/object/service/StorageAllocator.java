package org.hpcclab.msc.object.service;

import org.bson.types.ObjectId;
import org.hpcclab.msc.object.OcConfig;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

@ApplicationScoped
public class StorageAllocator {

  @Inject
  OcConfig config;

  public void allocate(MscObject object) {
    if (object.getId() == null) object.setId(new ObjectId());
    try {
      object.getState().setUrl(new URL(config.s3PrefixUrl(), object.getId().toHexString()).toString());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
