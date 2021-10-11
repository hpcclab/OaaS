package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.OaasObject;
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
  Uni<List<OaasObject>> list();

  @POST
  Uni<OaasObject> create(@Valid OaasObject creating);

  @GET
  @Path("{id}")
  Uni<OaasObject> get(String id);

  @POST
  @Path("{id}/binds")
  Uni<OaasObject> bindFunction(String id,
                               List<String> funcNames);


  @POST
  @Path("{id}/exec")
  Uni<OaasObject> activeFuncCall(String id, @Valid  FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<OaasObject> reactiveFuncCall(String id, @Valid  FunctionCallRequest request);

  @GET
  @Path("{id}/exec-context")
  Uni<FunctionExecContext> loadExecutionContext(String id);
}
