package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.repository.ObjectRepoManager;

/**
 * @author Pawissanutt
 */
public class RepoCtxLoader implements CtxLoader {

  final ObjectRepoManager objManager;
  final ClassControllerRegistry registry;
  final ProtoObjectMapper protoObjectMapper;

  public RepoCtxLoader(ObjectRepoManager objManager,
                       ClassControllerRegistry registry,
                       ProtoObjectMapper protoObjectMapper) {
    this.objManager = objManager;
    this.registry = registry;
    this.protoObjectMapper = protoObjectMapper;
  }


  @Override
  public Uni<InvocationCtx> load(InvocationRequest request) {
    var ctx = new InvocationCtx();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    ctx.setInputs(request.inputObjects());
    ctx.setInitTime(System.currentTimeMillis());
    Uni<InvocationCtx> uni = Uni.createFrom().item(ctx);
    var classController = registry.getClassController(request.cls());
    if (classController==null)
      throw InvocationException.notFoundCls400(request.cls());
    var cls = classController.getCls();
    if (request.main()!=null && !request.main().isEmpty()) {
      var repo = objManager.getOrCreate(cls);
      uni = uni.flatMap(ctx2 -> repo.async().getAsync(request.main())
        .map(ctx2::setMain)
      );
    }
    return uni;
  }

  @Override
  public Uni<InvocationCtx> load(ProtoInvocationRequest request) {
    return load(protoObjectMapper.fromProto(request));
  }
}
