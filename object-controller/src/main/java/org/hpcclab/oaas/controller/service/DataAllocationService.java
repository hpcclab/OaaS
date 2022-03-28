package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/allocate")
public interface DataAllocationService {
  @POST
  Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests);
}
