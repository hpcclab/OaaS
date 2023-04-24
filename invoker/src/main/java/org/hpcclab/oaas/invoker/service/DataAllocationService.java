package org.hpcclab.oaas.invoker.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import java.util.List;

@Path("/allocate")
@ApplicationScoped
public interface DataAllocationService {
  @POST
  @Operation(hidden = true)
  Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests);
}
