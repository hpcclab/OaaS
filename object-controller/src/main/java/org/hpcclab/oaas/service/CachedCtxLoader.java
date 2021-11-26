package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.function.OaasWorkflowStep;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.repository.OaasObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class CachedCtxLoader {
  @Inject
  Mutiny.SessionFactory sf;
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository clsRepo;


  public Uni<FunctionExecContext> loadCtx(FunctionCallRequest request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    return loadFunctionWithCls(request.getFunctionName())
      .onItem().ifNull()
      .failWith(() -> NoStackException.notFoundCls400(request.getFunctionName()))
      .flatMap(func -> {
        ctx.setFunction(func);
        return objectRepo.loadObject(request.getTarget());
      })
      .map(ctx::setMain)
      .flatMap(ignore -> clsRepo.loadCls(ctx.getMain().getCls().getName()))
      .map(cls -> {
        var main = ctx.getMain();
        main.setCls(cls);
        var binding = main.findFunction(request.getFunctionName());
        if (binding.isEmpty()) throw new NoStackException(
          "No function(" + request.getFunctionName() + ") on object", 400);
        return ctx.setMain(main)
          .setFunctionAccess(binding.get().getAccess());
      })
      .flatMap(ignore -> objectRepo.loadObjects(request.getAdditionalInputs()))
      .map(ctx::setAdditionalInputs);
  }

  public Uni<OaasFunction> loadFunctionWithCls(String name) {
    return funcRepo.loadFunction(name)
      .call(function -> {
          if (function.getOutputCls()==null) return Uni.createFrom().nullItem();
          return clsRepo.loadCls(function.getOutputCls().getName())
            .invoke(function::setOutputCls);
        }
      );
  }

  public Uni<FunctionExecContext> loadCtx(FunctionExecContext baseCtx,
                                          OaasWorkflowStep step) {
    var newCtx = new FunctionExecContext();
    newCtx.setParent(baseCtx);
    newCtx.setArgs(baseCtx.getArgs());

    return resolveTarget(baseCtx, step.getTarget())
      .invoke(newCtx::setMain)
      .chain(() -> clsRepo.loadCls(newCtx.getMain().getCls().getName()))
      .invoke(cls -> {
        var main = newCtx.getMain();
        main.setCls(cls);
        var binding = main.findFunction(step.getFuncName());
        if (binding.isEmpty()) throw new NoStackException(
          "No function(" + step.getFuncName() + ") on object", 400);
        newCtx.setFunctionAccess(binding.get().getAccess());
      })
      .chain(() -> resolveInputs(baseCtx, step))
      .chain(() -> loadFunctionWithCls(step.getFuncName()))
      .map(newCtx::setFunction);
  }

  public Uni<FunctionExecContext> resolveInputs(FunctionExecContext baseCtx,
                                                OaasWorkflowStep step) {
    return Multi.createFrom().iterable(step.getInputRefs())
      .onItem().transformToUniAndConcatenate(ref -> resolveTarget(baseCtx, ref))
      .collect().asList()
      .map(baseCtx::setAdditionalInputs);
  }

  public Uni<OaasObject> resolveTarget(FunctionExecContext baseCtx, String ref) {
    if (baseCtx.getWorkflowMap().containsKey(ref))
      return Uni.createFrom().item(baseCtx.getWorkflowMap().get(ref));
    var res = baseCtx.getMain().findMember(ref);
    if (res.isEmpty()) throw new NoStackException("Can not resolve '" + ref + "'");
    return objectRepo.loadObject(res.get().getId());
  }
}
