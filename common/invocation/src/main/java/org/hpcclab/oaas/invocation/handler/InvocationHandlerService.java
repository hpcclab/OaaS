package org.hpcclab.oaas.invocation.handler;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import org.eclipse.collections.impl.tuple.Tuples;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.invocation.InvocationQueueSender;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.MacroSpec;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.OalResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class InvocationHandlerService {
  private static final Logger logger = LoggerFactory.getLogger(InvocationHandlerService.class);
  UnifiedFunctionRouter router;
  InvocationExecutor invocationExecutor;
  InvocationQueueSender sender;
  InvocationValidator invocationValidator;
  IdGenerator idGenerator;

  @Inject
  public InvocationHandlerService(UnifiedFunctionRouter router,
                                  InvocationExecutor invocationExecutor,
                                  InvocationQueueSender sender,
                                  InvocationValidator invocationValidator,
                                  IdGenerator idGenerator) {
    this.router = router;
    this.invocationExecutor = invocationExecutor;
    this.sender = sender;
    this.invocationValidator = invocationValidator;
    this.idGenerator = idGenerator;
  }

  public Uni<OalResponse> syncInvoke(ObjectAccessLanguage oal) {
    var req = toRequest(oal)
      .build();
    return router.apply(req)
      .invoke(Unchecked.consumer(ctx -> {
        var func = ctx.getFunction();
        if (func.getType()==FunctionType.MACRO) {
          throw new InvocationException("Can not synchronous invoke to macro function", 400);
        }
        if (func.getDeploymentStatus().getCondition()!=DeploymentCondition.RUNNING) {
          throw new InvocationException("Function is not ready", 409);
        }
//        MutableList<Map.Entry<OaasObject, OaasObject>> waitForGraph =
//          Lists.mutable.empty();
//        MutableList<OaasObject> failDeps = Lists.mutable.empty();
//        if (!ctx.analyzeDeps(waitForGraph, failDeps))
//          throw InvocationException.notReady(waitForGraph, failDeps);
      }))
      .flatMap(ctx -> invocationExecutor.syncExec(ctx))
      .map(ctx -> OalResponse.builder()
        .target(ctx.getMain())
        .output(ctx.getOutput())
        .fbName(ctx.getFbName())
        .status(ctx.getNode().getStatus())
        .async(false)
        .build());
  }


  public Uni<OalResponse> asyncInvoke(ObjectAccessLanguage oal) {
    return
      invocationValidator.validate(oal)
        .map(ctx -> {
          var builder = toRequest(oal)
            .immutable(ctx.functionBinding().isForceImmutable())
            .macro(ctx.function().getType()==FunctionType.MACRO)
            .partKey(ctx.main()!=null ? ctx.main().getKey():null)
            .queTs(System.currentTimeMillis())
            .invId(idGenerator.generate());
          if (ctx.function().getType()==FunctionType.MACRO) {
            addMacroIds(builder, ctx.function().getMacro());
          } else if (ctx.functionBinding().getOutputCls()!=null) {
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
          .status(TaskStatus.DOING)
          .async(true)
          .build());
  }



  private void addMacroIds(InvocationRequest.InvocationRequestBuilder builder, MacroSpec dataflow) {
    var map = dataflow.getSteps().stream()
      .filter(step -> step.getAs()!=null)
      .map(step -> Map.entry(step.getAs(), idGenerator.generate()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    builder.macroIds(map);
    if (dataflow.getExport()!=null)
      builder.outId(map.get(dataflow.getExport()));
  }

  public InvocationRequest.InvocationRequestBuilder toRequest(ObjectAccessLanguage oal){
    return oal.toRequest()
      .invId(idGenerator.generate());
  }
//
//  public Uni<OaasObject> awaitCompletion(OaasObject obj,
//                                         Integer timeout) {
//    var status = obj.getStatus();
//    var ts = status.getTaskStatus();
//    if (!ts.isSubmitted() && !status.isInitWaitFor()) {
//      var uni1 = completionListener.wait(obj.getId(), timeout);
//      var uni2 = invocationExecutor.asyncSubmit(obj);
//      return Uni.combine().all().unis(uni1, uni2)
//        .asTuple()
//        .flatMap(v -> objectRepo.getAsync(obj.getId()));
//
//    }
//    return completionListener.wait(obj.getId(), timeout)
//      .flatMap(event -> objectRepo.getAsync(obj.getId()));
//  }
}
