package org.hpcclab.oaas.taskmanager.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.ContextLoader;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.InvocationQueueSender;
import org.hpcclab.oaas.invocation.InvocationValidator;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.IdGenerator;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.taskmanager.rest.OalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;

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

  public Uni<FunctionExecContext> syncInvoke(ObjectAccessLanguage oal) {
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

  public Uni<FunctionExecContext> asyncInvoke(ObjectAccessLanguage oal,
                                              boolean await,
                                              int timeout) {
    Uni<FunctionExecContext> uni = applyFunction(oal);
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
          var builder = InvocationRequest.builder()
            .target(ctx.oal().getTarget())
            .targetCls(ctx.oal().getTargetCls())
            .fbName(ctx.oal().getFunctionName())
            .args(ctx.oal().getArgs())
            .inputs(ctx.oal().getInputs())
            .immutable(ctx.functionBinding().isForceImmutable() || !ctx.function().getType().isAllowUpdateMain())
            .macro(ctx.function().getType() == FunctionType.MACRO)
            .function(ctx.function().getKey())
            .invId(idGenerator.generate());
          if (ctx.functionBinding().getOutputCls() != null) {
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
          .async(true)
          .build());
  }

  public Uni<FunctionExecContext> applyFunction(ObjectAccessLanguage oal) {
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
