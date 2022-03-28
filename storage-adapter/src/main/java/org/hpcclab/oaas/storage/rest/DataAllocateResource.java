package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Multi;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.storage.adapter.S3Adapter;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/allocate")
public class DataAllocateResource {
  @Inject
  S3Adapter s3Adapter;

  @POST
  public Multi<DataAllocateResponse> allocate(List<DataAllocateRequest> requests) {
    return Multi.createFrom().iterable(requests)
      .onItem().transformToUniAndConcatenate(req ->
        s3Adapter.allocate(req)
          .map(map -> new DataAllocateResponse(req.getOid(), map)
          )
      );
  }
}
