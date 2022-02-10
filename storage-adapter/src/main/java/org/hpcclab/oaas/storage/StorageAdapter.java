package org.hpcclab.oaas.storage;

import io.smallrye.mutiny.Uni;

import javax.ws.rs.core.Response;

public interface StorageAdapter {
  Uni<Response> get(DataAccessRequest crc);
  Uni<Response> put(DataAccessRequest crc);
  Uni<Response> delete(DataAccessRequest crc);
}
