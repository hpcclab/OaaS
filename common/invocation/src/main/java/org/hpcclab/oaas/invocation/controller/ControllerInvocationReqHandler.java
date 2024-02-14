package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.id.IdGenerator;

/**
 * @author Pawissanutt
 */
public class ControllerInvocationReqHandler implements InvocationReqHandler {
  final ClassControllerRegistry classControllerRegistry;
  final CtxLoader ctxLoader;
  final IdGenerator idGenerator;

  public ControllerInvocationReqHandler(ClassControllerRegistry classControllerRegistry,
                                        CtxLoader ctxLoader,
                                        IdGenerator idGenerator) {
    this.classControllerRegistry = classControllerRegistry;
    this.ctxLoader = ctxLoader;
    this.idGenerator = idGenerator;
  }

  @Override
  public Uni<InvocationResponse> syncInvoke(ObjectAccessLanguage oal) {
    var req = toRequest(oal).build();
    return syncInvoke(req);
  }

  @Override
  public Uni<InvocationResponse> syncInvoke(InvocationRequest request) {
    return ctxLoader.load(request)
      .flatMap(ctx -> {
        var con = classControllerRegistry.getClassController(request.cls());
        return con.invoke(ctx);
      })
      .map(ctx -> ctx.createResponse()
        .async(false)
        .build());
  }

  @Override
  public Uni<InvocationResponse> asyncInvoke(ObjectAccessLanguage oal) {
    var con = classControllerRegistry.getClassController(oal.getCls());
    var ctx = con.validate(oal);
    return con.enqueue(ctx.request())
      .map(v -> InvocationResponse.builder()
        .invId(ctx.request().invId())
        .output(new OObject().setId(ctx.request().outId()))
        .fb(ctx.fnBind().getName())
        .macroIds(ctx.request().macroIds())
        .status(InvocationStatus.QUEUE)
        .async(true)
        .build());
  }


  public InvocationRequest.InvocationRequestBuilder toRequest(ObjectAccessLanguage oal) {
    return oal.toRequest()
      .invId(idGenerator.generate());
  }
}
