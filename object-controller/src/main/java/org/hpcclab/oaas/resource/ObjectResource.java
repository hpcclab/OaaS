package org.hpcclab.oaas.resource;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.*;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.handler.FunctionRouter;
import org.hpcclab.oaas.service.ObjectService;
import org.hpcclab.oaas.model.DeepOaasObjectDto;
import org.hpcclab.oaas.model.FunctionCallRequest;
import org.hpcclab.oaas.model.OaasFunctionBindingDto;
import org.hpcclab.oaas.model.OaasObjectDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  OaasObjectRepository objectRepo;
//  @Inject
//  OaasFuncRepository funcRepo;
  @Inject
  FunctionRouter functionRouter;
  @Inject
  OaasMapper oaasMapper;

  public Uni<List<OaasObjectDto>> list() {
    return objectRepo.list()
      .map(oaasMapper::toObject);
  }

  @ReactiveTransactional
  public Uni<OaasObjectDto> create(OaasObjectDto creating) {
    LOGGER.info("create {} ", Json.encodePrettily(creating));
    return objectRepo.createRootAndPersist(creating)
      .map(oaasMapper::toObject)
      .onFailure().invoke(e -> LOGGER.error("error",e));
  }


  public Uni<OaasObjectDto> get(String id) {
    var uuid = UUID.fromString(id);
    return objectRepo.getById(uuid)
      .onItem().ifNull().failWith(NotFoundException::new)
      .map(oaasMapper::toObject);
  }

//  @ReactiveTransactional
  public Uni<DeepOaasObjectDto> getDeep(String id) {
    return objectRepo.getDeep(UUID.fromString(id))
      .map(oaasMapper::deep);
  }

  public Uni<OaasObject> getFullGraph(String id) {
    return objectRepo.getDeep(UUID.fromString(id));
  }

  public Uni<OaasObjectDto> bindFunction(String id,
                                         List<OaasFunctionBindingDto> bindingDtoList) {
    return objectRepo.bindFunction(UUID.fromString(id), bindingDtoList)
      .map(oaasMapper::toObject);
  }

  @ReactiveTransactional
  public Uni<OaasObjectDto> activeFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.activeCall(request.setTarget(UUID.fromString(id)))
      .map(oaasMapper::toObject);
  }

  @ReactiveTransactional
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
