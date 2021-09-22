package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.function.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.repository.MscObjectRepository;
import org.hpcclab.msc.object.service.ContextLoader;
import org.hpcclab.msc.object.handler.FunctionRouter;
import org.hpcclab.msc.object.service.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger( ObjectResource.class );
  @Inject
  MscObjectRepository objectRepo;
  @Inject
  MscFuncRepository funcRepo;
  @Inject
  FunctionRouter functionRouter;
  @Inject
  ContextLoader contextLoader;

  public Uni<List<MscObject>> list() {
    return objectRepo.listAll();
  }

  public Uni<MscObject> create(MscObject creating) {
    return objectRepo.createRootAndPersist(creating);
  }


  public Uni<MscObject> get(String id) {
    ObjectId oid = new ObjectId(id);
    return objectRepo.findById(oid)
      .map(o -> {
        if (o != null)
          return o;
        throw new NotFoundException();
      });
  }


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

  public Uni<MscObject> reactiveFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.reactiveCall(request.setTarget(new ObjectId(id)));
  }

  @Override
  public Uni<FunctionExecContext> loadExecutionContext(String id) {
    ObjectId oid = new ObjectId(id);
    return objectRepo.findById(oid)
      .flatMap(obj -> {
        if (obj == null) throw new NotFoundException();
        var origin = obj.getOrigin();
        return contextLoader.load(FunctionCallRequest.from(origin));
      });
  }
}
