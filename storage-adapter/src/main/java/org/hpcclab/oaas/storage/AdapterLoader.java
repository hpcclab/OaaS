package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.storage.adapter.S3Adapter;
import org.hpcclab.oaas.storage.adapter.StorageAdapter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class AdapterLoader {
  Map<String, StorageAdapter> adapterMap = Map.of();
  @Inject
  S3Adapter s3Adapter;

  @PostConstruct
  void setup() {
    adapterMap = Map.of("s3", s3Adapter);
  }
  public StorageAdapter load(String key,
                             OaasClass cls) {
    return s3Adapter;
  }
}
