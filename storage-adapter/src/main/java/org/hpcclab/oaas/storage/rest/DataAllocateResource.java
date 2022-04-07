package org.hpcclab.oaas.storage.rest;

import io.smallrye.mutiny.Multi;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;
import org.hpcclab.oaas.storage.AdapterLoader;
import org.hpcclab.oaas.storage.adapter.S3Adapter;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/allocate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DataAllocateResource {
  @Inject
  AdapterLoader adapterLoader;

  @POST
  public Multi<DataAllocateResponse> allocate(List<DataAllocateRequest> requests) {
    return Multi.createFrom().iterable(requests)
      .onItem().transformToUniAndConcatenate(req ->
        adapterLoader.aggregatedAllocate(req)
          .map(map -> new DataAllocateResponse(req.getOid(), map)
          )
      );
  }
}
