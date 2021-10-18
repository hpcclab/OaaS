package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.*;
import org.hpcclab.msc.object.entity.object.OaasObject;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
  @Inject
  OaasMapper oaasMapper;

  public Uni<List<OaasObjectDto>> list() {
    return objectRepo.listAll()
      .map(oaasMapper::toObject);
  }

  public Uni<OaasObjectDto> create(OaasObjectDto creating) {
    return objectRepo.createRootAndPersist(creating)
      .map(oaasMapper::toObject);
  }


  public Uni<OaasObjectDto> get(String id) {
//    ObjectId oid = new ObjectId(id);
    var uuid = UUID.fromString(id);
    return objectRepo.findById(uuid)
      .onItem().ifNull().failWith(NotFoundException::new)
      .map(oaasMapper::toObject);
  }

  public Uni<DeepOaasObjectDto> getDeep(String id) {
    return objectRepo.getTree(UUID.fromString(id))
      .map(oaasMapper::deep);
  }

  public Uni<OaasObjectDto> bindFunction(String id,
                                         List<OaasFunctionBindingDto> bindingDtoList) {
    return objectRepo.bindFunction(UUID.fromString(id), bindingDtoList)
      .map(oaasMapper::toObject);
  }

  public Uni<OaasObjectDto> activeFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.activeCall(request.setTarget(UUID.fromString(id)))
      .map(oaasMapper::toObject);
  }

  public Uni<OaasObjectDto> reactiveFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.reactiveCall(request.setTarget(UUID.fromString(id)))
      .map(oaasMapper::toObject);
  }

//  @Override
//  public Uni<FunctionExecContext> loadExecutionContext(String id) {
//    return objectRepo.findById(UUID.fromString(id))
//      .flatMap(obj -> {
//        if (obj==null) throw new NotFoundException();
//        var origin = obj.getOrigin();
//        return contextLoader.load(FunctionCallRequest.from(origin));
//      });
//  }
}
