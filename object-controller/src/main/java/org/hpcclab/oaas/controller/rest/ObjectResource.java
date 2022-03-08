package org.hpcclab.oaas.controller.rest;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.proto.TaskCompletion;
import org.hpcclab.oaas.repository.TaskCompletionRepository;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.object.DeepOaasObject;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.AggregateRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.*;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  TaskCompletionRepository completionRepo;
//  @Inject
//  FunctionRouter functionRouter;
//  @Inject
//  TaskExecutionService resourceRequestService;
  @Inject
  AggregateRepository aggregateRepo;

  public Uni<List<OaasObject>> list(Integer page, Integer size) {
    if (page==null) page = 0;
    if (size==null) size = 100;
    var list = objectRepo.pagination(page, size);
    return Uni.createFrom().item(list);
  }

  public Uni<OaasObject> create(OaasObject creating) {
    return objectRepo.createRootAndPersist(creating)
      .onFailure().invoke(e -> LOGGER.error("error", e));
  }

  public Uni<OaasObject> get(String id) {
    var uuid = UUID.fromString(id);
    return objectRepo.getAsync(uuid)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

//  public Uni<List<Map<String, OaasObjectOrigin>>> getOrigin(String id, Integer deep) {
//    return objectRepo.getOriginAsync(UUID.fromString(id), deep);
//  }

  public Uni<DeepOaasObject> getDeep(String id) {
    return objectRepo.getDeep(UUID.fromString(id));
  }

//  public Uni<TaskContext> getTaskContext(String id) {
//    return aggregateRepo.getTaskContextAsync(UUID.fromString(id));
//  }

//  public Uni<OaasObject> activeFuncCall(String id, ObjectAccessExpression request) {
//    request.setTarget(UUID.fromString(id));
//    return functionRouter.functionCall(request)
//      .map(FunctionExecContext::getOutput)
//      .call(out -> resourceRequestService.request(new TaskExecRequest()
//        .setId(out.getId().toString())));
//  }
//
//  @Blocking
//  public Uni<OaasObject> reactiveFuncCall(String id, ObjectAccessExpression request) {
//    request.setTarget(UUID.fromString(id));
//    return functionRouter.functionCall(request)
//      .map(FunctionExecContext::getOutput);
//  }

  @Override
  public Uni<TaskCompletion> getCompletion(String id) {
    var uuid = UUID.fromString(id);
    return completionRepo.getAsync(uuid)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
