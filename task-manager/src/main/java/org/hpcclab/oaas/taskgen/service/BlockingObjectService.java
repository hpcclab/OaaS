package org.hpcclab.oaas.taskgen.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.TaskContext;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
@RegisterRestClient(configKey = "BlockingObjectService")
public interface BlockingObjectService {

  @GET
  @Path("{id}/origin")
  List<Map<String, OaasObjectOrigin>> getOrigin(String id,
                                                @QueryParam("deep") Integer deep);

  @GET
  @Path("{id}/context")
  TaskContext getTaskContext(String id);
}
