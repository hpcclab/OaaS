package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.validation.Valid;
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
  Uni<MscObject> create(@Valid MscObject creating);

  @GET
  @Path("{id}")
  Uni<MscObject> get(String id);

  @POST
  @Path("{id}/binds")
  Uni<MscObject> bindFunction(String id,
                              List<String> funcNames);


  @POST
  @Path("{id}/exec")
  Uni<MscObject> activeFuncCall(String id,@Valid  FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<MscObject> reactiveFuncCall(String id,@Valid  FunctionCallRequest request);

  @GET
  @Path("{id}/exec-context")
  Uni<FunctionExecContext> loadExecutionContext(String id);
}
