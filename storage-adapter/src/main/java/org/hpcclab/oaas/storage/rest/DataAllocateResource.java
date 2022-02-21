package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.storage.adapter.S3Adapter;
import org.jboss.resteasy.reactive.RestQuery;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Path("/allocate")
public class DataAllocateResource {
  @Inject
  S3Adapter s3Adapter;

  @POST
  public Uni<DataAllocateResponse> allocate(DataAllocateRequest request) {
    return s3Adapter.allocate(request)
      .map(map -> new DataAllocateResponse(
        request.getOid(),
        map
      ));
  }
}
