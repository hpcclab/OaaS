package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.ObjectRepoManager;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class RepoCtxLoader implements CtxLoader {

  final ObjectRepoManager objManager;
  final ClassControllerRegistry registry;

  @Inject
  public RepoCtxLoader(ObjectRepoManager objManager, ClassControllerRegistry registry) {
    this.objManager = objManager;
    this.registry = registry;
  }


  @Override
  public Uni<InvocationCtx> load(InvocationRequest request) {
    var ctx = new InvocationCtx();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    ctx.setInputs(request.inputObjects());
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

//    if (request.inputs() != null && !request.inputs().isEmpty()) {
//      var functionController = classController.getFunctionController(request.fb());
//      if (functionController == null)
//        throw InvocationException.notFoundFnInCls(request.fb(), cls.getKey());
//      var fb = functionController.getFunctionBinding();
//      uni = uni.flatMap(ctx2 -> {
//          var zipped = Lists.fixedSize.ofAll(request.inputs())
//            .zip(fb.getInputTypes());
//          return Multi.createFrom().iterable(zipped)
//            .onItem().transformToUniAndConcatenate(pair ->
//              load(registry, pair.getTwo(), pair.getOne()))
//            .collect().asList()
//            .map(ctx2::setInputs);
//        });
//    }
    return uni;
  }

//  private Uni<OObject> load(ClassControllerRegistry registry,
//                            String clsKey,
//                            String id) {
//    var classController = registry.getClassController(clsKey);
//    if (classController == null)
//      throw InvocationException.notFoundCls400(clsKey);
//    var cls = classController.getCls();
//    return objManager.getOrCreate(cls)
//      .async()
//      .getAsync(id);
//  }

}
