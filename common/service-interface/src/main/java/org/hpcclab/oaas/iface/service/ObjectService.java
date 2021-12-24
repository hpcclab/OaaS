package org.hpcclab.oaas.iface.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.object.DeepOaasObject;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.*;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.proto.OaasObject;

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
  Uni<List<OaasObject>> list(@QueryParam("page") Integer page,
                             @QueryParam("size") Integer size);

  @POST
  Uni<OaasObject> create(@Valid @NotNull OaasObject creating);

  @GET
  @Path("{id}")
  Uni<OaasObject> get(String id);


  @GET
  @Path("{id}/origin")
  Uni<List<Map<String, OaasObjectOrigin>>> getOrigin(String id,
                                                   @DefaultValue("1")
                               @QueryParam("deep") Integer deep);

  @GET
  @Path("{id}/deep")
  Uni<DeepOaasObject> getDeep(String id);

  @GET
  @Path("{id}/context")
  Uni<TaskContext> getTaskContext(String id);

  @POST
  @Path("{id}/exec")
  Uni<OaasObject> activeFuncCall(String id, FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<OaasObject> reactiveFuncCall(String id, FunctionCallRequest request);

}
