package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionCallRequest;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.SubFunctionCall;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.security.PublicKey;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContextLoader {
  @Inject
  MscObjectRepository objectRepo;
  @Inject
  MscFuncRepository funcRepo;


  public Uni<FunctionExecContext> load(FunctionCallRequest request) {
    var oUni = objectRepo.findById(request.getTarget());
    var fUni = funcRepo.findByName(request.getFunctionName());
    var aUni = objectRepo.listByIds(request.getAdditionalInputs());
    return Uni.combine().all().unis(oUni,fUni,aUni)
      .combinedWith((o,f,a) -> new FunctionExecContext()
        .setRequest(request)
        .setTarget(o)
        .setFunction(f)
        .setAdditionalInputs(a)
        .setArgs(request.getArgs())
      )
      .flatMap(context -> {
        if (context.getTarget().getType() == MscObject.Type.COMPOUND) {
          return loadMembers(context.getTarget())
            .map(context::setMembers);
        } else {
          return Uni.createFrom().item(context);
        }
      })
      .flatMap(context -> {
        if (context.getFunction().getType() == MscFunction.Type.MACRO) {
          return loadMembers(context.getFunction())
            .map(context::setSubFunctions);
        } else {
          return Uni.createFrom().item(context);
        }
      });
  }

  public Uni<Map<String, MscObject>> loadMembers(MscObject main) {
    return objectRepo.listByIds(main.getMembers().values())
      .map(objList -> main.getMembers().entrySet()
        .stream()
        .map(e -> Map.entry(e.getKey(), objList.stream().filter(o -> o.getId().equals(e.getValue())).findFirst().orElseThrow()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
  }

  public Uni<Map<String, MscFunction>> loadMembers(MscFunction function) {
    var funcNames = function.getSubFunctions()
      .values()
      .stream()
      .map(SubFunctionCall::getFuncName)
      .collect(Collectors.toSet());
    return funcRepo.listByNames(funcNames)
      .map(objList -> objList.stream()
        .map(f -> Map.entry(f.getName(), f))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
  }
}
