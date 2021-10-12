package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.hpcclab.msc.object.repository.OaasObjectRepository;
import org.hpcclab.msc.object.service.ContextLoader;
import org.hpcclab.msc.object.handler.FunctionRouter;
import org.hpcclab.msc.object.service.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  FunctionRouter functionRouter;
  @Inject
  ContextLoader contextLoader;

  public Uni<List<OaasObject>> list() {
    return objectRepo.listAll();
  }

  public Uni<OaasObject> create(OaasObject creating) {
    return objectRepo.createRootAndPersist(creating);
  }


  public Uni<OaasObject> get(String id) {
//    ObjectId oid = new ObjectId(id);
    var uuid = UUID.fromString(id);
    return objectRepo.findById(uuid)
      .map(o -> {
        if (o!=null)
          return o;
        throw new NotFoundException();
      });
  }


  public Uni<OaasObject> bindFunction(String id,
                                      List<String> funcNames) {

    var uuid = UUID.fromString(id);
    var oUni = objectRepo.findById(uuid);
    var fmUni = funcRepo.listByNames(funcNames);
    return Uni.combine().all()
      .unis(oUni, fmUni)
      .asTuple()
      .flatMap(tuple -> {
        LOGGER.info("get tuple {} {}", tuple.getItem1(), tuple.getItem2());
        var o = tuple.getItem1();
        var fm = tuple.getItem2();
        if (o==null) {
          throw new NotFoundException("Not found object");
        }
        if (o.getFunctions()==null) o.setFunctions(new HashSet<>());
        for (OaasFunction value : fm) {
          o.getFunctions()
            .add(value);
        }
        return objectRepo.persistAndFlush(o);
      });
  }

  @Override
  public Uni<OaasObject> activeFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.activeCall(request.setTarget(UUID.fromString(id)));
  }

  public Uni<OaasObject> reactiveFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.reactiveCall(request.setTarget(UUID.fromString(id)));
  }

  @Override
  public Uni<FunctionExecContext> loadExecutionContext(String id) {
    return objectRepo.findById(UUID.fromString(id))
      .flatMap(obj -> {
        if (obj==null) throw new NotFoundException();
        var origin = obj.getOrigin();
        return contextLoader.load(FunctionCallRequest.from(origin));
      });
  }
}
