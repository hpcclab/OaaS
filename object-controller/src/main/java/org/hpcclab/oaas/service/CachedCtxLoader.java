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
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.hpcclab.oaas.repository.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;
import java.util.stream.Stream;

@ApplicationScoped
public class CachedCtxLoader {
  @Inject
  IfnpOaasObjectRepository objectRepo;
  @Inject
  IfnpOaasFuncRepository funcRepo;
  @Inject
  IfnpOaasClassRepository clsRepo;


  public Uni<FunctionExecContext> loadCtx(FunctionCallRequest request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    return objectRepo.getAsync(request.getTarget())
      .map(ctx::setMain)
      .map(ignore -> setClsAndFunc(ctx, request.getFunctionName()))
      .flatMap(ignore -> objectRepo.listByIds(request.getAdditionalInputs()))
      .map(ctx::setAdditionalInputs);
  }

  public FunctionExecContext setClsAndFunc(FunctionExecContext ctx, String funcName) {
    var func = funcRepo.get(funcName);
    if (func == null)
      throw NoStackException.notFoundCls400(funcName);
    var outputClass = clsRepo.get(func.getOutputCls());
    ctx.setFunction(func);
    ctx.setOutputCls(outputClass);

    var main = ctx.getMain();
    var mainCls = clsRepo.get(main.getCls());
    ctx.setMainCls(mainCls);
    var binding = clsRepo.findFunction(
      mainCls.getName(), funcName);
    if (binding.isEmpty()) throw new NoStackException(
      "Function(" + funcName + ") can be not executed on object", 400);
    ctx.setFunctionAccess(binding.get().getAccess());
    return ctx;
  }

  public Uni<FunctionExecContext> loadCtx(FunctionExecContext baseCtx,
                                          OaasWorkflowStep step) {
    var newCtx = new FunctionExecContext();
    newCtx.setParent(baseCtx);
    newCtx.setArgs(baseCtx.getArgs());

    return resolveTarget(baseCtx, step.getTarget())
      .invoke(newCtx::setMain)
      .map(ignore -> setClsAndFunc(newCtx, step.getFuncName()))
      .chain(() -> resolveInputs(baseCtx, step));
  }

  public Uni<FunctionExecContext> resolveInputs(FunctionExecContext baseCtx,
                                                OaasWorkflowStep step) {
    return Multi.createFrom().iterable(step.getInputRefs())
      .onItem().transformToUniAndConcatenate(ref -> resolveTarget(baseCtx, ref))
      .collect().asList()
      .map(baseCtx::setAdditionalInputs);
  }

  public Uni<OaasObjectPb> resolveTarget(FunctionExecContext baseCtx, String ref) {
    if (baseCtx.getWorkflowMap().containsKey(ref))
      return Uni.createFrom().item(baseCtx.getWorkflowMap().get(ref));
    var res = baseCtx.getMain().findMember(ref);
    if (res.isEmpty()) throw new NoStackException("Can not resolve '" + ref + "'");
    return objectRepo.getAsync(res.get().getObject());
  }
}
