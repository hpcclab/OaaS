package org.hpcclab.oaas.resource;

import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;
import org.hpcclab.oaas.handler.FunctionRouter;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.*;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.service.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  OaasMapper oaasMapper;

  public Uni<List<OaasObjectDto>> list() {
    return objectRepo.list()
      .map(oaasMapper::toObject);
  }

  @ReactiveTransactional
  public Uni<OaasObjectDto> create(OaasObjectDto creating) {
//    if (creating==null) throw new BadRequestException();
    return objectRepo.createRootAndPersist(creating)
      .map(oaasMapper::toObject)
      .onFailure().invoke(e -> LOGGER.error("error", e));
  }


  public Uni<OaasObjectDto> get(String id) {
    var uuid = UUID.fromString(id);
    return objectRepo.getById(uuid)
      .onItem().ifNull().failWith(NotFoundException::new)
      .map(oaasMapper::toObject);
  }

  @Override
  public Uni<List<Map<String, OaasObjectOrigin>>> getOrigin(String id, Integer deep) {
    List<Map<String, OaasObjectOrigin>> results = new ArrayList<>();
    return Multi.createFrom().range(0, deep)
      .call(i -> {
        if (i==0) {
          return objectRepo.findById(UUID.fromString(id))
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

          return objectRepo.listByIds(ids)
            .map(objs -> objs.stream()
              .map(o -> Map.entry(o.getId().toString(), o.getOrigin()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            )
            .invoke(map -> results.add(i, map));
        }
      })
      .collect().last()
      .map(v -> results);
  }

  //  @ReactiveTransactional
  public Uni<DeepOaasObjectDto> getDeep(String id) {
    return objectRepo.getDeep(UUID.fromString(id))
      .map(oaasMapper::deep);
  }

  public Uni<TaskContext> getTaskContext(String id) {
    return objectRepo.getById(UUID.fromString(id))
      .flatMap(main -> {
        var tc = new TaskContext();
        tc.setOutput(oaasMapper.toObject(main));
        var funcName = main.getOrigin().getFuncName();
        var uni = funcRepo.findByName(funcName)
          .map(func -> tc.setFunction(oaasMapper.toFunc(func)));
        if (main.getOrigin().getParentId()!=null) {
          uni = uni.flatMap(t -> objectRepo.getById(main.getOrigin().getParentId()))
            .map(parent -> tc.setParent(oaasMapper.toObject(parent)));
        }
        uni = uni.flatMap(t -> objectRepo.listFetchByIds(main.getOrigin().getAdditionalInputs()))
          .map(parent -> tc.setAdditionalInputs(oaasMapper.toObject(parent)));
        return uni
          .map(f -> tc);
      });
  }

  //  public Uni<OaasObject> getFullGraph(String id) {
//    return objectRepo.getDeep(UUID.fromString(id));
//  }

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
