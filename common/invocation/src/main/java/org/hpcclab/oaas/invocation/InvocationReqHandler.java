package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
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
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvocationReqHandler {
  private static final Logger logger = LoggerFactory.getLogger(InvocationReqHandler.class);
  UnifiedFunctionRouter router;
  InvocationExecutor invocationExecutor;
  InvocationQueueProducer producer;
  InvocationValidator invocationValidator;
  IdGenerator idGenerator;

  @Inject
  public InvocationReqHandler(UnifiedFunctionRouter router,
                              InvocationExecutor invocationExecutor,
                              InvocationQueueProducer producer,
                              InvocationValidator invocationValidator,
                              IdGenerator idGenerator) {
    this.router = router;
    this.invocationExecutor = invocationExecutor;
    this.producer = producer;
    this.invocationValidator = invocationValidator;
    this.idGenerator = idGenerator;
  }

  public Uni<OalResponse> syncInvoke(ObjectAccessLanguage oal) {
    var req = toRequest(oal)
      .build();
    return router.apply(req)
      .flatMap(Unchecked.function(ctx -> {
        var func = ctx.getFunction();
        if (func.getType()==FunctionType.MACRO) {
          throw new InvocationException("Can not synchronous invoke to macro func", 400);
        }
        if (func.getType()==FunctionType.LOGICAL) {
          return Uni.createFrom().item(ctx);
        }
        if (func.getDeploymentStatus().getCondition()!=DeploymentCondition.RUNNING) {
          throw new InvocationException("Function is not ready", 409);
        }
        return invocationExecutor.syncExec(ctx);
      }))
      .map(ctx -> ctx.createResponse().async(false).build());
  }


  public Uni<OalResponse> asyncInvoke(ObjectAccessLanguage oal) {
    return
      invocationValidator.validate(oal)
        .map(ctx -> {
          var builder = toRequest(oal)
            .immutable(ctx.funcBind().isForceImmutable())
            .macro(ctx.func().getType()==FunctionType.MACRO)
            .partKey(ctx.main()!=null ? ctx.main().getKey():null)
            .queTs(System.currentTimeMillis())
            .cls(ctx.cls().getKey())
            .invId(idGenerator.generate());
          if (ctx.func().getType()==FunctionType.MACRO) {
            addMacroIds(builder, ctx.func().getMacro());
          } else if (ctx.funcBind().getOutputCls()!=null) {
            builder.outId(idGenerator.generate());
          }

          return Tuples.pair(ctx, builder.build());
        })
        .call(pair -> producer.offer(pair.getTwo()))
        .map(pair -> OalResponse.builder()
          .invId(pair.getTwo().invId())
          .output(new OaasObject().setId(pair.getTwo().outId()))
          .main(pair.getOne().main())
          .fb(pair.getOne().funcBind().getName())
          .macroIds(pair.getTwo().macroIds())
          .status(TaskStatus.DOING)
          .async(true)
          .build());
  }



  private void addMacroIds(InvocationRequest.InvocationRequestBuilder builder, MacroSpec dataflow) {
    var map = Lists.fixedSize.ofAll(dataflow.getSteps())
      .select(step -> step.getAs()!=null)
      .collect(step -> Map.entry(step.getAs(), idGenerator.generate()))
      .toMap(Map.Entry::getKey, Map.Entry::getValue);
    builder.macroIds(DSMap.wrap(map));
    if (dataflow.getExport()!=null)
      builder.outId(map.get(dataflow.getExport()));
  }

  public InvocationRequest.InvocationRequestBuilder toRequest(ObjectAccessLanguage oal){
    return oal.toRequest()
      .invId(idGenerator.generate());
  }
}
