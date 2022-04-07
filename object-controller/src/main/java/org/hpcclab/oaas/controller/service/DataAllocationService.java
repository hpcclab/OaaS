package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.data.DataAllocateResponse;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

@Path("/allocate")
@RegisterRestClient(configKey = "allocation-api")
public interface DataAllocationService {
  @POST
  Uni<List<DataAllocateResponse>> allocate(List<DataAllocateRequest> requests);
}
