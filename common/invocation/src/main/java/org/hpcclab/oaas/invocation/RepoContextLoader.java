package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Getter
@ApplicationScoped
public class RepoContextLoader implements ContextLoader {
  private static final Logger logger = LoggerFactory.getLogger(RepoContextLoader.class);

  EntityRepository<String, OaasObject> objectRepo;
  EntityRepository<String, OaasFunction> funcRepo;
  EntityRepository<String, OaasClass> clsRepo;
  EntityRepository<String, InvocationNode> invNodeRepo;

  @Inject
  public RepoContextLoader(EntityRepository<String, OaasObject> objectRepo,
                           EntityRepository<String, OaasFunction> funcRepo,
                           EntityRepository<String, OaasClass> clsRepo,
                           EntityRepository<String, InvocationNode> invNodeRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
    this.invNodeRepo = invNodeRepo;
  }

  @Override
  public Uni<InvocationContext> loadCtxAsync(InvocationRequest request) {
    var ctx = new InvocationContext();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    Uni<?> uni;
    if (request.main() != null) {
      uni = objectRepo.getAsync(request.main())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject400(request.main()))
        .invoke(ctx::setMain);
    } else {
      uni = Uni.createFrom().item(ctx);
    }
    uni = uni.map(ignore -> loadClsAndFunc(ctx, request.fb()))
      .flatMap(ignore -> objectRepo.orderedListAsync(request.inputs()))
      .invoke(ctx::setInputs);

    if (request.preloadingNode()) {
      uni = uni.flatMap(__ -> invNodeRepo.getAsync(request.invId()))
        .invoke(invNode -> {
          if (invNode!=null)
            ctx.setNode(invNode);
        });
    }

    return uni.replaceWith(ctx);
  }

  public InvocationContext loadClsAndFunc(InvocationContext ctx, String fbName) {
    var main = ctx.getMain();
    var mainClsKey = main != null? main.getCls() : ctx.getRequest().cls();
    var mainCls = clsRepo.get(mainClsKey);
    Set<String> clsKeys = Sets.mutable.of(mainClsKey);

    if (mainCls==null)
      throw StdOaasException.format("Can not find class '%s'", main.getCls());
    ctx.setMainCls(mainCls);
    ctx.getInputs().stream().map(OaasObject::getCls).forEach(clsKeys::add);
    var clsMap = clsRepo.list(clsKeys);
    ctx.setClsMap(clsMap);

    var binding = mainCls
      .findFunction(fbName);
    if (binding==null)
      throw FunctionValidationException.noFunction(mainClsKey, fbName);
    clsKeys.add(binding.getOutputCls());
    ctx.setFb(binding);

    var func = funcRepo.get(binding.getFunction());
    if (func==null)
      throw StdOaasException.notFoundFunc(binding.getFunction(), 500);
    ctx.setFunction(func);
    if (binding.getOutputCls()!=null) {
      var outputClass = clsRepo.get(binding.getOutputCls());
      ctx.setOutputCls(outputClass);
    }
    return ctx;
  }

  @Override
  public Uni<OaasObject> resolveObj(InvocationContext baseCtx, String ref) {
    if (ref.startsWith("$.")) {
      var res = baseCtx.getMain().findReference(ref.substring(2));
      if (res.isPresent()) {
        var obj = baseCtx.getMainRefs().get(res.get().getName());
        if (obj!=null)
          return Uni.createFrom().item(obj);
        var id = res.get().getObjId();
        return objectRepo.getAsync(id)
          .onItem().ifNull()
          .failWith(() -> FunctionValidationException.cannotResolveMacro(ref, "object not found"))
          .invoke(o -> baseCtx.getMainRefs().put(id, o));
      }
    } else {
      return Uni.createFrom().item(baseCtx.resolveDataflowRef(ref));
    }

    throw FunctionValidationException.cannotResolveMacro(ref, null);
  }

}
