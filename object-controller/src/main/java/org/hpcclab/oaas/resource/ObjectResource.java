package org.hpcclab.oaas.resource;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.handler.FunctionRouter;
import org.hpcclab.oaas.iface.service.ObjectService;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.object.DeepOaasObjectDto;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.hpcclab.oaas.repository.IfnpOaasFuncRepository;
import org.hpcclab.oaas.repository.IfnpOaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class ObjectResource implements ObjectService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectResource.class);
  @Inject
  IfnpOaasObjectRepository objectRepo;
  @Inject
  IfnpOaasFuncRepository funcRepo;
  @Inject
  FunctionRouter functionRouter;

  public Uni<List<OaasObjectPb>> list(Integer page, Integer size) {
    if (page==null) page = 0;
    if (size==null) size = 100;
    var list = objectRepo.pagination(page, size);
    return Uni.createFrom().item(list);
  }

  public Uni<OaasObjectPb> create(OaasObjectPb creating) {
    return objectRepo.createRootAndPersist(creating)
      .onFailure().invoke(e -> LOGGER.error("error", e));
  }

  public Uni<OaasObjectPb> get(String id) {
    var uuid = UUID.fromString(id);
    return objectRepo.getAsync(uuid)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  public Uni<List<Map<String, OaasObjectOrigin>>> getOrigin(String id, Integer deep) {
    List<Map<String, OaasObjectOrigin>> results = new ArrayList<>();
    return Multi.createFrom().range(0, deep)
      .call(i -> {
        if (i==0) {
          return objectRepo.getAsync(UUID.fromString(id))
            .onItem().ifNull().failWith(NotFoundException::new)
            .map(o -> Map.of(id, o.getOrigin()))
            .invoke(map -> results.add(i, map));
        } else {
          Set<UUID> ids = results.get(i - 1).values()
            .stream()
            .filter(o -> o.getParentId()!=null)
            .flatMap(origin -> Stream.concat(Stream.of(origin.getParentId()), origin.getAdditionalInputs()
              .stream())
            )
            .collect(Collectors.toSet());

          if (ids.isEmpty()) {
            results.add(i, Map.of());
            return Uni.createFrom().item(Map.of());
          }

          return objectRepo.listAsync(ids)
            .map(objs -> objs.values().stream()
              .map(o -> Map.entry(o.getId().toString(), o.getOrigin()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            )
            .invoke(map -> results.add(i, map));
        }
      })
      .collect().last()
      .map(v -> results);
  }

  public Uni<DeepOaasObjectDto> getDeep(String id) {
    return objectRepo.getDeep(UUID.fromString(id));
  }

  @Blocking
  public Uni<TaskContext> getTaskContext(String id) {
    var main = objectRepo.get(UUID.fromString(id));
    var tc = new TaskContext();
    tc.setOutput(main);
    var funcName = main.getOrigin().getFuncName();
    var function = funcRepo.get(funcName);
    tc.setFunction(function);
    var inputs = objectRepo.listByIds(main.getOrigin().getAdditionalInputs());
    tc.setAdditionalInputs(inputs);
    if (main.getOrigin().getParentId()!=null) {
      var parent = objectRepo.get(main.getOrigin().getParentId());
      tc.setParent(parent);
    }
    return Uni.createFrom().item(tc);
  }

  public Uni<OaasObjectPb> activeFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.activeCall(request.setTarget(UUID.fromString(id)));
  }

  @Blocking
  public Uni<OaasObjectPb> reactiveFuncCall(String id, FunctionCallRequest request) {
    return functionRouter.reactiveCall(request.setTarget(UUID.fromString(id)));
  }
}
