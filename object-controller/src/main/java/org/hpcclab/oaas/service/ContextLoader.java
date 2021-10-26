package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasWorkflowStep;
import org.hpcclab.oaas.entity.object.OaasCompoundMember;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.FunctionCallRequest;
import org.hpcclab.oaas.model.FunctionExecContext;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Stream;

@RequestScoped
public class ContextLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(ContextLoader.class);
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  Mutiny.Session session;
  @Inject
  OaasMapper oaasMapper;

  public Uni<FunctionExecContext> loadCtx(FunctionCallRequest request) {
    return objectRepo
      .findById(request.getTarget())
      .call(obj -> Mutiny.fetch(obj.getCls()))
      .call(obj -> Mutiny.fetch(obj.getCls().getFunctions()))
      .call(obj -> Mutiny.fetch(obj.getFunctions()))
      .flatMap(object -> {
        var ctx = new FunctionExecContext().setEntry(object)
          .setMain(object)
          .setArgs(request.getArgs());
        if (request.getAdditionalInputs()!=null && !request.getAdditionalInputs().isEmpty()) {
          return objectRepo.listByIds(request.getAdditionalInputs())
            .map(ctx::setAdditionalInputs);
        } else {
          return Uni.createFrom().item(ctx);
        }
      })
      .call(ctx -> {
        if (ctx.getMain().getType() == OaasObject.ObjectType.COMPOUND){
          return Mutiny.fetch(ctx.getMain().getMembers());
        }
        return Uni.createFrom().item(ctx);
      })
      .map(ctx -> {
        var fnName = request.getFunctionName();
        var binding = Stream.concat(
            ctx.getMain().getFunctions().stream(),
            ctx.getMain().getCls().getFunctions().stream()
          )
          .filter(fb -> fb.getFunction().getName().equals(fnName))
          .findAny()
          .orElseThrow(() -> new NoStackException("No function with name '%s' available in object '%s'"
            .formatted(fnName, ctx.getMain().getId().toString())
          ));
        return ctx.setFunction(binding.getFunction())
          .setFunctionAccess(binding.getAccess());
      })
      .call(fec -> Mutiny.fetch(fec.getFunction()))
      .call(fec -> Mutiny.fetch(fec.getFunction().getOutputCls()))
      .invoke(() -> LOGGER.debug("successfully load context of '{}'", request.getTarget()));
  }

  public Uni<FunctionExecContext> loadCtx(FunctionExecContext baseCtx,
                                          OaasObject main,
                                          OaasWorkflowStep step) {
    var newCtx = oaasMapper.copy(baseCtx);
    return objectRepo.getSession()
      .flatMap(ss -> objectRepo.getDeep(main.getId())
//        .invoke(newMain -> LOGGER.info("main {}", newMain.getCls()))
//        .call(newMain -> ss.refresh(newMain))
//        .call(newMain -> ss.fetch(newMain.getCls()))
//        .call(newMain -> ss.fetch(newMain.getCls().getFunctions()))
//        .call(newMain -> ss.fetch(newMain.getFunctions()))
        .flatMap(newMain -> {
          LOGGER.info("main (after fetch) {} {}", newMain, newMain.getCls());
          newCtx.setMain(main);
          var functionBinding = Stream.concat(
              main.getFunctions().stream(),
              main.getCls().getFunctions().stream()
            )
            .filter(fb -> fb.getFunction().getName().equals(step.getFuncName()))
            .findFirst().orElseThrow();
          return Mutiny.fetch(functionBinding.getFunction().getOutputCls());
        })
        .map(o -> newCtx)
      );
  }
}
