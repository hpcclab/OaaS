package org.hpcclab.oaas.repository.storage;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import java.net.URI;
import java.util.UUID;

@ApplicationScoped
@Default
public class DefaultStorageAllocator implements StorageAllocator {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStorageAllocator.class);

  @ConfigProperty(name = "oaas.sa.s3PrefixUrl",
    defaultValue = "http://localhost:8080/")
  String s3PrefixUrl;

  @PostConstruct
  void setup(){
    if (!s3PrefixUrl.endsWith("/")) {
      s3PrefixUrl += '/';
    }
  }

  public void allocate(OaasObject object) {
    if (object.getId()==null) object.setId(UUID.randomUUID());
    object.getState().setBaseUrl(
      URI.create(s3PrefixUrl).resolve(object.getId().toString()).toString()
    );
  }
}
