package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/allocate")
public interface DataAllocationService {
  @POST
  Uni<DataAllocateResponse> allocate(DataAllocateRequest request);
}
