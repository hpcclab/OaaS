package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.model.data.DataAccessRequest;

import org.hpcclab.oaas.model.data.DataAllocateRequest;

import java.util.Map;

public interface StorageAdapter {
  Map<String,String> allocate(DataAllocateRequest request);
  String get(DataAccessRequest dar);
}
