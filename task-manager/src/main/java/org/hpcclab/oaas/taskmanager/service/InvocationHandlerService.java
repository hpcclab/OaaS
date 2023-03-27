package org.hpcclab.oaas.taskmanager.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.InvocationQueueSender;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.IdGenerator;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.taskmanager.rest.OalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class InvocationHandlerService {
  private static final Logger logger = LoggerFactory.getLogger(InvocationHandlerService.class);
  @Inject
  UnifiedFunctionRouter router;
  @Inject
  ObjectRepository objectRepo;
  @Inject
  InvocationExecutor invocationExecutor;
  @Inject
  ObjectCompletionListener completionListener;
  @Inject
  InvocationQueueSender sender;
  @Inject
  InvocationValidator invocationValidator;
  @Inject
  IdGenerator idGenerator;

  public Uni<InvApplyingContext> syncInvoke(ObjectAccessLanguage oal) {
    return applyFunction(oal)
      .invoke(Unchecked.consumer(ctx -> {
        var func = ctx.getFunction();
        if (func.getType()==FunctionType.MACRO) {
          throw new InvocationException("Can not synchronous invoke to macro function", 400);
        }
        if (func.getDeploymentStatus().getCondition()!=DeploymentCondition.RUNNING) {
          throw new InvocationException("Function is not ready", 409);
        }
        MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
          Lists.mutable.empty();
        MutableList<OaasObject> failDeps = Lists.mutable.empty();
        if (!ctx.analyzeDeps(waitForGraph, failDeps))
          throw InvocationException.notReady(waitForGraph, failDeps);
      }))
      .flatMap(ctx -> invocationExecutor.syncExec(ctx));
  }

  public Uni<InvApplyingContext> asyncInvoke(ObjectAccessLanguage oal,
                                             boolean await,
                                             int timeout) {
    Uni<InvApplyingContext> uni = applyFunction(oal);
    return uni
      .flatMap(ctx -> {
        if (completionListener.enabled() && await && ctx.getOutput()!=null) {
          var id = ctx.getOutput().getId();
          var uni1 = completionListener.wait(id, timeout);
          var uni2 = invocationExecutor.asyncSubmit(ctx);
          return Uni.combine().all().unis(uni1, uni2)
            .asTuple()
            .replaceWith(ctx);
        }
        return invocationExecutor.asyncSubmit(ctx)
          .replaceWith(ctx);
      });
  }

  public Uni<OalResponse> asyncInvoke(ObjectAccessLanguage oal) {
    return
      invocationValidator.validate(oal)
        .map(ctx -> {
          var targetCls = ctx.oal().getTarget() != null?
            ctx.mainCls().getKey():
            ctx.oal().getTargetCls();
          var builder = InvocationRequest.builder()
            .target(ctx.oal().getTarget())
            .targetCls(targetCls)
            .fbName(ctx.oal().getFunctionName())
            .args(ctx.oal().getArgs())
            .inputs(ctx.oal().getInputs())
            .immutable(ctx.functionBinding().isForceImmutable() || !ctx.function().getType().isMutable())
            .macro(ctx.function().getType() == FunctionType.MACRO)
            .function(ctx.function().getKey())
            .partKey(ctx.main() != null? ctx.main().getKey(): null)
            .invId(idGenerator.generate());
          if (ctx.function().getType() == FunctionType.MACRO) {
            addMacroIds(builder, ctx.function().getMacro());
          } else if (ctx.functionBinding().getOutputCls() != null) {
            builder.outId(idGenerator.generate());
          }

          return Tuples.pair(ctx, builder.build());
        })
        .call(pair -> sender.send(pair.getTwo()))
        .map(pair -> OalResponse.builder()
          .invId(pair.getTwo().invId())
          .output(new OaasObject().setId(pair.getTwo().outId()))
          .target(pair.getOne().main())
          .fbName(pair.getOne().functionBinding().getName())
          .macroIds(pair.getTwo().macroIds())
          .async(true)
          .build());
  }

  private void addMacroIds(InvocationRequest.InvocationRequestBuilder builder, MacroConfig dataflow) {
    var map = dataflow.getSteps().stream()
      .filter(step -> step.getAs() != null)
      .map(step -> Map.entry(step.getAs(), idGenerator.generate()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    builder.macroIds(map);
    if (dataflow.getExport() != null)
      builder.outId(map.get(dataflow.getExport()));
  }

  public Uni<InvApplyingContext> applyFunction(ObjectAccessLanguage oal) {
    var uni = router.apply(oal);
    if (logger.isDebugEnabled()) {
      uni = uni
        .invoke(() -> logger.debug("Applying function '{}' succeed", oal));
    }
    return uni;
  }

  public Uni<OaasObject> awaitCompletion(OaasObject obj,
                                         Integer timeout) {
    var status = obj.getStatus();
    var ts = status.getTaskStatus();
    if (!ts.isSubmitted() && !status.isInitWaitFor()) {
      var uni1 = completionListener.wait(obj.getId(), timeout);
      var uni2 = invocationExecutor.asyncSubmit(obj);
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(v -> objectRepo.getAsync(obj.getId()));

    }
    return completionListener.wait(obj.getId(), timeout)
      .flatMap(event -> objectRepo.getAsync(obj.getId()));
  }
}
