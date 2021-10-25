package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.object.OaasCompoundMember;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.FunctionCallRequest;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

@ApplicationScoped
public class ContextLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextLoader.class);
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;

  public Uni<FunctionExecContext> loadCtx(FunctionCallRequest request) {
    return objectRepo
      .getDeep(request.getTarget())
      .flatMap(object -> {
        var fec = new FunctionExecContext().setEntry(object)
          .setMain(object)
          .setArgs(request.getArgs());
        if (request.getAdditionalInputs() != null && !request.getAdditionalInputs().isEmpty()) {
          return objectRepo.listByIds(request.getAdditionalInputs())
            .map(fec::setAdditionalInputs);
        } else{
          return Uni.createFrom().item(fec);
        }
      })
      .flatMap(fec -> {
        if (fec.getMain().getType() == OaasObject.ObjectType.COMPOUND) {
         return Multi.createFrom().iterable(fec.getMain().getMembers())
            .onItem().transformToUniAndMerge(member -> objectRepo.refreshWithDeep(member.getObject())
               .map(obj -> new OaasCompoundMember().setName(member.getName()).setObject(obj))
            )
            .collect().last().map(member -> fec);
        } else {
          return Uni.createFrom().item(fec);
        }
      })
      .map(fec -> {
        var fnName = request.getFunctionName();
        var binding = Stream.concat(
            fec.getMain().getFunctions().stream(),
            fec.getMain().getCls().getFunctions().stream()
          )
          .filter(fb -> fb.getFunction().getName().equals(fnName))
          .findAny()
          .orElseThrow(() -> new NoStackException("No function with name '%s' available in object '%s'"
            .formatted(fnName, fec.getMain().getId().toString())
          ));
        return fec.setFunction(binding.getFunction())
          .setFunctionAccess(binding.getAccess());
      })
      .invoke(()-> LOGGER.debug("successfully load context of '{}'", request.getTarget()));
  }

  public Uni<Map<String, OaasFunction>> loadWorkflow(OaasFunction function) {
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
