package org.hpcclab.msc.object.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.model.ErrorMessage;
import org.hpcclab.msc.object.repository.MscFuncRepository;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/functions")
public class FunctionResource {
  @Inject
  MscFuncRepository funcRepo;

  @GET
  public Uni<List<MscFunction>> list() {
    return funcRepo.listAll();
  }

  @POST
  public Uni<Response> create(MscFunction mscFunction) {
    return funcRepo.findByName(mscFunction.getName())
      .flatMap(fn -> {
        if (fn != null) {
          return Uni.createFrom()
            .item(Response.status(HttpResponseStatus.CONFLICT.code())
              .entity(new ErrorMessage().setMsg("Function with this name already exist."))
              .build()
            );
        }
        return funcRepo.persist(mscFunction)
          .map(f -> Response.ok(f).build());
      });
  }

  @GET
  @Path("{funcName}")
  public Uni<Response> get(String funcName) {
    return funcRepo.findByName(funcName)
      .map(f -> {
        if (f!=null)
          return Response.ok(f).build();
        else
          return Response.status(404).build();
      });
  }
}
