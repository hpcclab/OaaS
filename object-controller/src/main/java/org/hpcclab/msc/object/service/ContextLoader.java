package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.entity.function.SubFunctionCall;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.hpcclab.msc.object.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContextLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger( ContextLoader.class );
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;

  public Uni<FunctionExecContext> load(FunctionCallRequest request) {
    var oUni = objectRepo.findById(request.getTarget());
    var fUni = funcRepo.findByName(request.getFunctionName());
    var aUni = objectRepo.listByIds(request.getAdditionalInputs());
    return Uni.combine().all().unis(oUni,fUni,aUni)
      .combinedWith((o,f,a) -> new FunctionExecContext()
        .setMain(o)
        .setFunction(f)
        .setAdditionalInputs(a)
        .setArgs(request.getArgs())
      )
      .invoke(context -> {
        if (context.getMain() == null)
          throw new NoStackException("Not found object with id = " + request.getTarget().toString());
        if (context.getFunction() == null)
          throw new NoStackException("Not found function with name = " + request.getFunctionName());
      })
      .flatMap(context -> {
        if (context.getMain().getType() == OaasObject.ObjectType.COMPOUND) {
          return loadMembers(context.getMain())
            .map(context::setMembers);
        } else {
          return Uni.createFrom().item(context);
        }
      })
      .flatMap(context -> {
        if (context.getFunction().getType() == OaasFunction.FuncType.MACRO) {
          return loadMembers(context.getFunction())
            .map(context::setSubFunctions);
        } else {
          return Uni.createFrom().item(context);
        }
      });
  }

  public Uni<Map<String, OaasObject>> loadMembers(OaasObject main) {
    return null;
//    return Multi.createFrom().iterable(main.getMembers())
//      .onItem().transformToUniAndMerge(member -> objectRepo.findById(member)
//        .map(object -> {
//          if (object == null)
//            throw new NoStackException("Not found object with id " + entry.getValue().toString());
//          return Map.entry(entry.getKey(), object);
//        }))
//      .collect()
//      .asList()
//      .map(l -> l.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
//
//    return objectRepo.listByIds(main.getMembers().values())
//      .map(objList -> {
//        LOGGER.info("objList {}", Json.encodePrettily(objList));
//        LOGGER.info("members {}", Json.encodePrettily(main.getMembers()));
//        return main.getMembers().entrySet()
//            .stream()
//            .map(e -> Map.entry(e.getKey(), objList.stream().filter(o -> o.getId().equals(e.getValue())).findFirst(). orElseThrow()))
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//        }
//      );
  }

  public Uni<Map<String, OaasFunction>> loadMembers(OaasFunction function) {
//    var funcNames = function.getSubFunctions()
//      .values()
//      .stream()
//      .map(SubFunctionCall::getFuncName)
//      .collect(Collectors.toSet());
//    return funcRepo.listByNames(funcNames)
//      .map(objList -> {
//        if (objList.size() != funcNames.size()) {
//          var l = new HashSet<>(funcNames);
//          l.removeAll(objList.stream().map(OaasFunction::getName).collect(Collectors.toSet()));
//          throw new NoStackException("Can not load function " + l);
//        }
//        return objList.stream()
//            .map(f -> Map.entry(f.getName(), f))
//            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//      }
//      );
    return null;
  }
}
