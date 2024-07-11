package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.time.Duration;
import java.util.Optional;

/**
 * @author Pawissanutt
 */
public abstract class AbstractFunctionController implements FunctionController {

  protected final IdGenerator idGenerator;
  protected final ObjectMapper mapper;
  protected MetricFactory metricFactory;
  protected FunctionBinding functionBinding;
  protected OFunction function;
  protected OClass cls;
  protected OClass outputCls;
  protected JsonObject customConfig;
  protected MetricFactory.MetricTimer invocationTimer;

  protected AbstractFunctionController(IdGenerator idGenerator,
                                       ObjectMapper mapper) {
    this.idGenerator = idGenerator;
    this.mapper = mapper;
  }

  @Override
  public void bind(MetricFactory metricFactory,
                   FunctionBinding functionBinding,
                   OFunction function,
                   OClass cls,
                   OClass outputCls) {
    this.metricFactory = metricFactory;
    this.functionBinding = functionBinding;
    this.function = function;
    this.cls = cls;
    this.outputCls = outputCls;
    this.customConfig = Optional.ofNullable(function.getConfig())
      .map(OFunctionConfig::getCustom)
      .map(JsonObject::new)
      .orElseGet(JsonObject::new)
      .copy();
    var override = functionBinding.getOverride()!=null ?
      new JsonObject(functionBinding.getOverride()):new JsonObject();
    this.customConfig.mergeIn(override);
    afterBind();
  }

  protected void afterBind() {
  }

  @Override
  public Uni<InvocationCtx> invoke(InvocationCtx context) {
    if (invocationTimer==null) {
      invocationTimer = metricFactory.createInvocationTimer(
        cls.getKey(),
        functionBinding.getName(),
        functionBinding.getFunction()
      );
    }
    validate(context);
    Uni<InvocationCtx> exec = exec(context);
    return exec
      .invoke(ctx -> invocationTimer.recordTime(Duration.ofMillis(System.currentTimeMillis() - ctx.getInitTime())));
  }

  protected abstract void validate(InvocationCtx ctx);

  protected abstract Uni<InvocationCtx> exec(InvocationCtx ctx);

  @Override
  public OFunction getFunction() {
    return function;
  }

  @Override
  public FunctionBinding getFunctionBinding() {
    return functionBinding;
  }

  @Override
  public OClass getCls() {
    return cls;
  }

  @Override
  public OClass getOutputCls() {
    return outputCls;
  }
}
