package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.applier.UnifiedFunctionRouter;
import org.hpcclab.oaas.invocation.validate.InvocationValidator;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.MacroSpec;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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

  public Uni<InvocationResponse> syncInvoke(ObjectAccessLanguage oal) {
    var req = toRequest(oal)
      .build();
    return syncInvoke(req)
      .map(ctx -> ctx.createResponse().async(false).build());
  }
  public Uni<InvocationContext> syncInvoke(InvocationRequest request) {
    logger.debug("syncInvoke {}", request);
    return router.apply(request)
      .flatMap(Unchecked.function(ctx -> {
        var func = ctx.getFunction();
        if (func.getType()==FunctionType.MACRO) {
          throw new InvocationException("Can not synchronous invoke to macro func", 400);
        }
        if (func.getType()==FunctionType.LOGICAL) {
          return Uni.createFrom().item(ctx);
        }
        if (func.getStatus().getCondition()!=DeploymentCondition.RUNNING) {
          throw new InvocationException("Function is not ready", 409);
        }
        return invocationExecutor.syncExec(ctx);
      }));
  }


  public Uni<InvocationResponse> asyncInvoke(ObjectAccessLanguage oal) {
    record ReqAndCtx(InvocationRequest req, ValidationContext ctx) {}
    return
      invocationValidator.validate(oal)
        .map(ctx -> {
          var builder = toRequest(oal)
            .immutable(ctx.fnBind().isForceImmutable())
            .macro(ctx.func().getType()==FunctionType.MACRO)
            .partKey(ctx.main()!=null ? ctx.main().getKey():null)
            .queTs(System.currentTimeMillis())
            .cls(ctx.cls().getKey())
            .invId(idGenerator.generate());
          if (ctx.func().getType()==FunctionType.MACRO) {
            addMacroIds(builder, ctx.func().getMacro());
          } else if (ctx.fnBind().getOutputCls()!=null) {
            builder.outId(idGenerator.generate());
          }

          return new ReqAndCtx(builder.build(), ctx);
        })
        .call(reqAndCtx -> producer.offer(reqAndCtx.req()))
        .map(reqAndCtx -> InvocationResponse.builder()
          .invId(reqAndCtx.req().invId())
          .output(new OObject().setId(reqAndCtx.req().outId()))
          .main(reqAndCtx.ctx.main())
          .fb(reqAndCtx.ctx.fnBind().getName())
          .macroIds(reqAndCtx.req.macroIds())
          .status(InvocationStatus.DOING)
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

  public String newId() {
    return idGenerator.generate();
  }
}
