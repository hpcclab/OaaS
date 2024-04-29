package org.hpcclab.oaas.invocation.controller;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pawissanutt
 */
public class CcInvocationReqHandler implements InvocationReqHandler {
  protected final ClassControllerRegistry classControllerRegistry;
  protected final CtxLoader ctxLoader;
  protected final IdGenerator idGenerator;
  protected final AtomicInteger inflight = new AtomicInteger(0);
  protected final int maxInflight;

  public CcInvocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                CtxLoader ctxLoader,
                                IdGenerator idGenerator,
                                int maxInflight) {
    this.classControllerRegistry = classControllerRegistry;
    this.ctxLoader = ctxLoader;
    this.idGenerator = idGenerator;
    this.maxInflight = maxInflight;
  }


  @Override
  public Uni<InvocationResponse> invoke(InvocationRequest request) {
    if (request.fb()==null || request.fb().isEmpty()) {
      return ctxLoader.load(request)
        .map(ctx -> ctx.createResponse()
          .async(false)
          .build());
    }

    int count = inflight.incrementAndGet();
    if (count>maxInflight) {
      inflight.decrementAndGet();
      throw new StdOaasException("too many requests", HttpResponseStatus.TOO_MANY_REQUESTS.code());
    }
    return ctxLoader.load(request)
      .flatMap(ctx -> {
        var con = classControllerRegistry.getClassController(request.cls());
        if (con==null) throw StdOaasException.notFoundCls400(request.cls());
        return con.invoke(ctx);
      })
      .map(ctx -> ctx.createResponse()
        .async(false)
        .build())
      .eventually(inflight::decrementAndGet);
  }


  @Override
  public Uni<InvocationResponse> enqueue(InvocationRequest request) {
    var con = classControllerRegistry.getClassController(request.cls());
    if (con==null) throw StdOaasException.notFoundCls400(request.cls());
    var ctx = con.validate(request);
    return con.enqueue(ctx.request())
      .map(v -> InvocationResponse.builder()
        .invId(ctx.request().invId())
        .output(new OObject().setId(ctx.request().outId()))
        .fb(ctx.fb()!=null ? ctx.fb().getName():"")
        .status(InvocationStatus.QUEUE)
        .async(true)
        .build());
  }

  class Counter{
    int count;

  }
}
