package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public interface ObjectService {
  @GET
  Uni<List<MscObject>> list();

  @POST
  Uni<MscObject> create(MscObject creating);

  @GET
  @Path("{id}")
  Uni<MscObject> get(String id);

  @POST
  @Path("{id}/binds")
  Uni<MscObject> bindFunction(String id,
                              List<String> funcNames);

  @POST
  @Path("{id}/rf-call")
  Uni<MscObject> reactiveFuncCall(String id, FunctionCallRequest request);
}
