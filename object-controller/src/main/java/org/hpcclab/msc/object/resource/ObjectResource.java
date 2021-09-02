package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.hpcclab.msc.object.service.FunctionRouter;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public class ObjectResource {
  private static final Logger LOGGER = LoggerFactory.getLogger( ObjectResource.class );
  @Inject
  MscObjectRepository objectRepo;
  @Inject
  MscFuncRepository funcRepo;
  @Inject
  FunctionRouter functionRouter;

  @GET
  public Uni<List<MscObject>> list() {
    return objectRepo.listAll();
  }

  @POST
  public Uni<MscObject> createRoot(MscObject creating) {
    return objectRepo.createRootAndPersist(creating);
  }

  @GET
  @Path("{id}")
  public Uni<Response> get(String id) {
    ObjectId oid = new ObjectId(id);
    return objectRepo.findById(oid)
      .map(o -> {
        if (o != null)
          return Response.ok(o).build();
        else
          return Response.status(404).build();
      });
  }

  @POST
  @Path("{id}/binds")
  public Uni<MscObject> bindFunction(String id,
                                     List<String> funcNames) {
    ObjectId oid = new ObjectId(id);
    var oUni = objectRepo.findById(oid);
    var fmUni = funcRepo.listByNames(funcNames);
    return Uni.combine().all()
      .unis(oUni,fmUni)
      .asTuple()
      .flatMap(tuple -> {
        LOGGER.info("get tuple {} {}", tuple.getItem1(), tuple.getItem2());
        var o = tuple.getItem1();
        var fm = tuple.getItem2();
        if (tuple.getItem1() == null) {
          throw new NotFoundException("Not found object");
        }
        if (tuple.getItem2() == null) {
          throw new NotFoundException("Not found function");
        }
        if (o.getFunctions()== null) o.setFunctions(new ArrayList<>());
        for (MscFunction value : fm) {
          o.getFunctions()
            .add(value.getName());
        }
        return objectRepo.update(o);
      });
  }

//  @POST
//  @Path("{id}/af-call/{funcName}")
//  public Uni<MscObject> activeFuncCall(String id,
//                                         String funcName,
//                                         Map<String, String> args) {
//    ObjectId oid = new ObjectId(id);
//    var oUni = objectRepo.findById(oid);
//    var fUni = funcRepo.findByName(funcName);
//    return Uni.combine().all()
//      .unis(oUni,fUni)
//      .asTuple()
//      .flatMap(tuple -> {
//        if (tuple.getItem1() == null || tuple.getItem2() == null)
//          throw new NotFoundException();
//        return functionCaller.activeFuncCall(tuple.getItem1(),
//          tuple.getItem2(), args);
//      });
//  }

  @POST
  @Path("{id}/rf-call")
  public Uni<MscObject> reactiveFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.reactiveCall(request.setTarget(new ObjectId(id)));
  }



  @ServerExceptionMapper
  public Response exceptionMapper(IllegalArgumentException illegalArgumentException) {
    return Response.status(404)
      .entity(new JsonObject()
        .put("msg", illegalArgumentException.getMessage()))
      .build();
  }

  @ServerExceptionMapper
  public Response exceptionMapper(WebApplicationException webApplicationException) {
    return Response.fromResponse(webApplicationException.getResponse())
      .entity(new JsonObject()
        .put("msg", webApplicationException.getMessage()))
      .build();
  }

  @ServerExceptionMapper
  public Response exceptionMapper(NoStackException noStackException) {
    return Response.status(noStackException.getCode())
      .entity(new JsonObject()
        .put("msg", noStackException.getMessage()))
      .build();
  }
}
