package org.hpcclab.oaas.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionCallRequest;
import org.hpcclab.oaas.model.function.OaasWorkflowStep;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.hpcclab.oaas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CachedCtxLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger( CachedCtxLoader.class );
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
      .flatMap(ignore -> setClsAndFuncAsync(ctx, request.getFunctionName()))
      .flatMap(ignore -> objectRepo.listByIdsAsync(request.getAdditionalInputs()))
      .map(ctx::setAdditionalInputs);
  }

  public FunctionExecContext loadCtxBlocking(FunctionCallRequest request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    var obj = objectRepo.get(request.getTarget());
    ctx.setMain(obj);
    setClsAndFunc(ctx, request.getFunctionName());
    var inputIds = request.getAdditionalInputs();
    var inputs =objectRepo.listByIds(inputIds);
    ctx.setAdditionalInputs(inputs);
    return ctx;
  }

  public Uni<FunctionExecContext> setClsAndFuncAsync(FunctionExecContext ctx,
                                                     String funcName){
    return funcRepo.getAsync(funcName)
      .map(ctx::setFunction)
      .flatMap(ignore -> {
        if (ctx.getFunction().getOutputCls() != null)
          return clsRepo.getAsync(ctx.getFunction().getOutputCls());
        return Uni.createFrom().nullItem();
      })
      .map(ctx::setOutputCls)
      .flatMap(ignore -> clsRepo.getAsync(ctx.getMain().getCls()))
      .map(ctx::setMainCls)
      .map(ignore -> {
        var binding = clsRepo.findFunction(
          ctx.getMainCls(), funcName);
        if (binding.isEmpty()) throw new NoStackException(
          "Function(" + funcName + ") can be not executed on object", 400);
        ctx.setFunctionAccess(binding.get().getAccess());
        return ctx;
      });
  }

  public FunctionExecContext setClsAndFunc(FunctionExecContext ctx, String funcName) {
    var func = funcRepo.get(funcName);
    if (func == null)
      throw NoStackException.notFoundCls400(funcName);
    ctx.setFunction(func);
    LOGGER.trace("func {}", func);
    if (func.getOutputCls() != null) {
      var outputClass = clsRepo.get(func.getOutputCls());
      ctx.setOutputCls(outputClass);
      LOGGER.trace("outputClass {}", outputClass);
    }

    var main = ctx.getMain();
    var mainCls = clsRepo.get(main.getCls());
    LOGGER.trace("mainCls {}", mainCls);
    ctx.setMainCls(mainCls);
    var binding = clsRepo.findFunction(
      mainCls, funcName);
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
      .flatMap(ignore -> setClsAndFuncAsync(newCtx, step.getFuncName()))
      .chain(() -> resolveInputs(newCtx, step));
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
