package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscObject;
import org.hpcclab.msc.object.model.RootMscObjectCreating;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public class ObjectResource {
  @Inject
  MscObjectRepository objectRepo;

  @GET
  public Uni<List<MscObject>> list() {
    return objectRepo.listAll();
  }

  @POST
  public Uni<MscObject> createRoot(RootMscObjectCreating creating) {
    return objectRepo.createRootAndPersist(creating);
  }

  @GET
  @Path("{id}")
  public Uni<Response> get(String id) {
    ObjectId oid = new ObjectId(id);
    return objectRepo.findByIdOptional(oid)
      .map(op -> {
        if (op.isPresent())
          return Response.ok(op.get()).build();
        else
          return Response.status(404).build();
      });
  }

  @POST
  @Path("{id}/lazy-func-call/{funcName}")
  public Uni<MscObject> lazyFuncCall(String id,
                                     String funcName,
                                     Map<String, String> args) {
    ObjectId oid = new ObjectId(id);
    return objectRepo.lazyFuncCall(oid, funcName, args);
  }



  @ServerExceptionMapper
  public Response exceptionMapper(IllegalArgumentException illegalArgumentException) {
    return Response.status(404)
      .build();
  }
}
