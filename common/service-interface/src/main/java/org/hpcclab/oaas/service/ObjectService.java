package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.DeepOaasObjectDto;
import org.hpcclab.oaas.model.FunctionCallRequest;
import org.hpcclab.oaas.model.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.OaasObjectDto;

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
  Uni<OaasObjectDto> activeFuncCall(String id, @Valid FunctionCallRequest request);

  @POST
  @Path("{id}/r-exec")
  Uni<OaasObjectDto> reactiveFuncCall(String id, @Valid  FunctionCallRequest request);

//  @GET
//  @Path("{id}/exec-context")
//  Uni<FunctionExecContext> loadExecutionContext(String id);
}
