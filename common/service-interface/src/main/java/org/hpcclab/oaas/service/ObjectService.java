package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public interface ObjectService {
  @GET
  Uni<List<OaasObjectDto>> list();

  @POST
  Uni<OaasObjectDto> create(@Valid @NotNull OaasObjectDto creating);

  @GET
  @Path("{id}")
  Uni<OaasObjectDto> get(String id);


  @GET
  @Path("{id}/origin")
  Uni<List<Map<String, OaasObjectOrigin>>> getOrigin(String id,
                                                   @DefaultValue("1")
                               @QueryParam("deep") Integer deep);

  @GET
  @Path("{id}/deep")
  Uni<DeepOaasObjectDto> getDeep(String id);

  @GET
  @Path("{id}/context")
  Uni<TaskContext> getTaskContext(String id);

  @POST
  @Path("{id}/binds")
  Uni<OaasObjectDto> bindFunction(String id,
                               List<OaasFunctionBindingDto> funcNames);

  @POST
  @Path("{id}/exec")
  Uni<OaasObjectDto> activeFuncCall(String id, @Valid FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<OaasObjectDto> reactiveFuncCall(String id, @Valid  FunctionCallRequest request);

}
