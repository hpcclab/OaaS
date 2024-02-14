package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.controller.InvocationCtx;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public abstract class AbstractFunctionController implements FunctionController {

  protected IdGenerator idGenerator;
  protected ObjectMapper mapper;

  protected FunctionBinding functionBinding;
  protected OFunction function;
  protected OClass cls;
  protected OClass outputCls;

  protected AbstractFunctionController(IdGenerator idGenerator,
                                       ObjectMapper mapper) {
    this.idGenerator = idGenerator;
    this.mapper = mapper;
  }

  @Override
  public void bind(FunctionBinding functionBinding,
                   OFunction function,
                   OClass cls,
                   OClass outputCls) {
    this.functionBinding = functionBinding;
    this.function = function;
    this.cls = cls;
    this.outputCls = outputCls;
    afterBind();
  }

  protected void afterBind() {
  }

  @Override
  public Uni<InvocationCtx> invoke(InvocationCtx context) {
    validate(context);
    return exec(context);
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
}
