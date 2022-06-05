package org.hpcclab.oaas.storage.adapter;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAccessRequest;

import javax.ws.rs.core.Response;
import java.util.Map;

public interface StorageAdapter {
  Uni<Map<String,String>> allocate(InternalDataAllocateRequest request);
  Uni<Response> get(DataAccessRequest dar);
//  Uni<Response> put(DataAccessRequest dar);
//  Uni<Response> delete(DataAccessRequest dar);
  String name();
}
