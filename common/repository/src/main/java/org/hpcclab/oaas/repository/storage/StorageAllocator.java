package org.hpcclab.oaas.repository.storage;

import org.hpcclab.oaas.model.proto.OaasObject;

public interface StorageAllocator {
  void allocate(OaasObject object);
}
