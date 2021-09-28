package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.ObjectResourceRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/resource-requests/")
public interface ResourceRequestService {

  @POST
  Uni<TaskFlow> request(ObjectResourceRequest request);
}
