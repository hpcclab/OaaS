package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.model.*;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public interface ObjectService {
  @GET
  Uni<List<OaasObjectDto>> list();

  @POST
  Uni<OaasObjectDto> create(@Valid OaasObjectDto creating);

  @GET
  @Path("{id}")
  Uni<OaasObjectDto> get(String id);

  @GET
  @Path("{id}/deep")
  Uni<DeepOaasObjectDto> getDeep(String id);

  @POST
  @Path("{id}/binds")
  Uni<OaasObjectDto> bindFunction(String id,
                               List<OaasFunctionBindingDto> funcNames);


  @POST
  @Path("{id}/exec")
  Uni<OaasObjectDto> activeFuncCall(String id, @Valid  FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<OaasObjectDto> reactiveFuncCall(String id, @Valid  FunctionCallRequest request);

//  @GET
//  @Path("{id}/exec-context")
//  Uni<FunctionExecContext> loadExecutionContext(String id);
}
