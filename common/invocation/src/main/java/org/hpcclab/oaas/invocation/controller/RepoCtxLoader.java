package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.ObjectRepoManager;

/**
 * @author Pawissanutt
 */
public class RepoCtxLoader implements CtxLoader {

  final ObjectRepoManager objManager;
  final ClassControllerRegistry registry;

  public RepoCtxLoader(ObjectRepoManager objManager,
                       ClassControllerRegistry registry) {
    this.objManager = objManager;
    this.registry = registry;
  }


  @Override
  public Uni<InvocationCtx> load(InvocationRequest request) {
    var ctx = new InvocationCtx();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    ctx.setInitTime(System.currentTimeMillis());
    Uni<InvocationCtx> uni = Uni.createFrom().item(ctx);
    var classController = registry.getClassController(request.cls());
    if (classController==null)
      throw StdOaasException.notFoundCls400(request.cls());
    var cls = classController.getCls();
    if (request.main()!=null && !request.main().isEmpty()) {
      var repo = objManager.getOrCreate(cls);
      uni = uni.flatMap(ctx2 -> repo.async().getAsync(request.main())
        .map(ctx2::setMain)
      );
    }
    return uni;
  }
}
