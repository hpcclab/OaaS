package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.ObjectResourceRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/resource-requests/")
public interface ResourceRequestService {

//  @POST
//  Uni<TaskFlow> request(ObjectResourceRequest request);
  @POST
  Uni<Void> request(ObjectResourceRequest request);
}
