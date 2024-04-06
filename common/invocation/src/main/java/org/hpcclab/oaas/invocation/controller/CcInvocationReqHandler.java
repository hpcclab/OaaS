package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class CcInvocationReqHandler implements InvocationReqHandler {
  protected final ClassControllerRegistry classControllerRegistry;
  protected final CtxLoader ctxLoader;
  protected final IdGenerator idGenerator;

  public CcInvocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                CtxLoader ctxLoader,
                                IdGenerator idGenerator) {
    this.classControllerRegistry = classControllerRegistry;
    this.ctxLoader = ctxLoader;
    this.idGenerator = idGenerator;
  }

  @Override
  public Uni<InvocationResponse> invoke(ObjectAccessLanguage oal) {
    var req = toRequest(oal).build();
    return invoke(req);
  }

  @Override
  public Uni<InvocationResponse> invoke(InvocationRequest request) {
    return ctxLoader.load(request)
      .flatMap(ctx -> {
        var con = classControllerRegistry.getClassController(request.cls());
        if (con == null) throw StdOaasException.notFoundCls400(request.cls());
        return con.invoke(ctx);
      })
      .map(ctx -> ctx.createResponse()
        .async(false)
        .build());
  }

  @Override
  public Uni<InvocationResponse> enqueue(ObjectAccessLanguage oal) {
    var con = classControllerRegistry.getClassController(oal.getCls());
    if (con == null) throw StdOaasException.notFoundCls400(oal.getCls());
    var ctx = con.validate(oal);
    return con.enqueue(ctx.request())
      .map(v -> InvocationResponse.builder()
        .invId(ctx.request().invId())
        .output(new OObject().setId(ctx.request().outId()))
        .fb(ctx.fb() != null? ctx.fb().getName() : "")
        .status(InvocationStatus.QUEUE)
        .async(true)
        .build());
  }


  public InvocationRequest.InvocationRequestBuilder toRequest(ObjectAccessLanguage oal) {
    return oal.toRequest()
      .invId(idGenerator.generate());
  }
}
