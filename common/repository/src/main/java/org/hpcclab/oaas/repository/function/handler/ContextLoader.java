package org.hpcclab.oaas.repository.function.handler;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasWorkflowStep;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;

@ApplicationScoped
public class ContextLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger( ContextLoader.class );
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository clsRepo;


  public Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLangauge request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    return objectRepo.getAsync(request.getTarget())
      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(request.getTarget()))
      .map(ctx::setMain)
//      .flatMap(ignore -> setClsAndFuncAsync(ctx, request.getFunctionName()))
      .map(ignore -> setClsAndFunc(ctx, request.getFunctionName()))
      .flatMap(ignore -> objectRepo.listByIdsAsync(request.getInputs()))
      .map(ctx::setAdditionalInputs);
  }

  public FunctionExecContext loadCtx(ObjectAccessLangauge request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    var obj = objectRepo.get(request.getTarget());
    ctx.setMain(obj);
    setClsAndFunc(ctx, request.getFunctionName());
    var inputIds = request.getInputs();
    var inputs =objectRepo.listByIds(inputIds);
    ctx.setAdditionalInputs(inputs);
    return ctx;
  }

//  public Uni<FunctionExecContext> setClsAndFuncAsync(FunctionExecContext ctx,
//                                                     String funcName){
//    return funcRepo.getAsync(funcName)
//      .onItem().ifNull().failWith(() -> NoStackException.notFoundFunc(funcName, 409))
//      .map(ctx::setFunction)
//      .flatMap(ignore -> {
//        if (ctx.getFunction().getOutputCls() != null)
//          return clsRepo.getAsync(ctx.getFunction().getOutputCls())
//            .onItem().ifNull().failWith(() -> NoStackException.notFoundCls(ctx.getFunction().getOutputCls(), 409));
//        return Uni.createFrom().nullItem();
//      })
//      .map(ctx::setOutputCls)
//      .flatMap(ignore -> clsRepo.getAsync(ctx.getMain().getCls()))
//      .map(ctx::setMainCls)
//      .map(Unchecked.function(ignore -> {
//        var binding = clsRepo.findFunction(
//          ctx.getMainCls(), funcName);
//        if (binding.isEmpty()) throw new NoStackException(
//          "Function(" + funcName + ") can be not executed on object", 400);
//        ctx.setFunctionAccess(binding.get().getAccess());
//        return ctx;
//      }));
//  }

  public FunctionExecContext setClsAndFunc(FunctionExecContext ctx, String funcName) {
    var main = ctx.getMain();
    var mainCls = clsRepo.get(main.getCls());
    ctx.setMainCls(mainCls);
    var binding = clsRepo.findFunction(
      mainCls, funcName);
    if (binding.isEmpty()) throw FunctionValidationException.noFunction(main.getId(), funcName);
    ctx.setFunctionAccess(binding.get().getAccess());

    var func = funcRepo.get(binding.get().getFunction());
    if (func == null)
      throw NoStackException.notFoundFunc400(funcName);
    ctx.setFunction(func);
    if (func.getOutputCls() != null) {
      var outputClass = clsRepo.get(func.getOutputCls());
      ctx.setOutputCls(outputClass);
    }
    return ctx;
  }

  public Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx,
                                               OaasWorkflowStep step) {
    var newCtx = new FunctionExecContext();
    newCtx.setParent(baseCtx);
    newCtx.setArgs(step.getArgs());
    if (step.getArgRefs() != null && !step.getArgRefs().isEmpty()) {
      var map = new HashMap<String,String>();
      for (var entry : step.getArgRefs().entrySet()) {
        var resolveArg = baseCtx.getArgs().get(entry.getValue());
        if (resolveArg == null) throw new FunctionValidationException(
          "Can not resolve args '%s' from step %s".formatted( entry.getValue(), step));
        map.put(entry.getKey(), resolveArg);
      }
      if (newCtx.getArgs() != null)
        newCtx.getArgs().putAll(map);
      else
        newCtx.setArgs(map);
    }

    return resolveTarget(baseCtx, step.getTarget())
      .invoke(newCtx::setMain)
//      .flatMap(ignore -> setClsAndFuncAsync(newCtx, step.getFuncName()))
      .map(ignore -> setClsAndFunc(newCtx, step.getFuncName()))
      .chain(() -> resolveInputs(newCtx, step));
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
    var res = baseCtx.getMain().findReference(ref);
    if (res.isEmpty()) throw new NoStackException("Can not resolve '" + ref + "'");
    return objectRepo.getAsync(res.get().getObject());
  }
}
