package org.hpcclab.oaas.sa.adapter;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAccessRequest;

import jakarta.ws.rs.core.Response;
import java.util.Map;

public interface StorageAdapter {
  Uni<Map<String,String>> allocate(InternalDataAllocateRequest request);
  Uni<Response> get(DataAccessRequest dar);
  String name();
}
