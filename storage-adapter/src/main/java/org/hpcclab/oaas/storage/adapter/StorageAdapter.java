package org.hpcclab.oaas.storage.adapter;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.storage.DataAccessRequest;

import javax.ws.rs.core.Response;
import java.util.List;

public interface StorageAdapter {
  Uni<Response> loadPutUrls(DataAccessRequest dar, List<String> keys);
  Uni<Response> get(DataAccessRequest dar);
  Uni<Response> put(DataAccessRequest dar);
  Uni<Response> delete(DataAccessRequest dar);
}
