package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

@Getter
@ApplicationScoped
public class RepoContextLoader implements ContextLoader {

  private static final Logger logger = LoggerFactory.getLogger(RepoContextLoader.class);

  ObjectRepoManager objManager;
  FunctionRepository funcRepo;
  ClassRepository clsRepo;
  InvRepoManager invRepoManager;

  @Inject
  public RepoContextLoader(ObjectRepoManager objectRepoManager,
                           FunctionRepository funcRepo,
                           ClassRepository clsRepo,
                           InvRepoManager invRepoManager) {
    this.objManager = objectRepoManager;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
    this.invRepoManager = invRepoManager;
  }

  @Override
  public Uni<InvocationContext> loadCtxAsync(InvocationRequest request) {
    var ctx = new InvocationContext();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    loadClsAndFunc(ctx, request.fb());
    Uni<?> uni;
    if (request.main() != null) {
      uni = objManager.getOrCreate(ctx.getMainCls())
        .async().getAsync(request.main())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject400(request.main()))
        .invoke(ctx::setMain);
    } else {
      uni = Uni.createFrom().item(ctx);
    }

    if (request.inputs() != null && !request.inputs().isEmpty()) {
      uni = uni.flatMap(__ -> {
        var zipped = Lists.fixedSize.ofAll(request.inputs())
          .zip(ctx.getFb().getInputTypes());
        return Multi.createFrom().iterable(zipped)
          .onItem().transformToUniAndConcatenate(pair ->
            load(pair.getTwo(), pair.getOne()))
          .collect().asList();
      })
        .invoke(ctx::setInputs);
    }

    if (request.preloadingNode()) {
      uni = uni.flatMap(__ -> invRepoManager.getOrCreate(request.cls())
          .async().getAsync(request.invId()))
        .invoke(invNode -> {
          if (invNode!=null)
            ctx.setNode(invNode);
        });
    }

    return uni.replaceWith(ctx);
  }

  private Uni<OObject> load(String clsKey, String id) {
    var cls = clsRepo.get(clsKey);
    return objManager.getOrCreate(cls)
      .async()
      .getAsync(id);
  }

  public InvocationContext loadClsAndFunc(InvocationContext ctx,
                                          String fbName) {
    var mainClsKey = Optional.ofNullable(ctx.getMain())
      .map(OObject::getCls)
      .orElseGet(() -> ctx.getRequest().cls());
    var mainCls = clsRepo.get(mainClsKey);
    Set<String> clsKeys = Sets.mutable.of(mainClsKey);

    if (mainCls==null)
      throw StdOaasException.format("Can not find class '%s'", mainClsKey);
    ctx.setMainCls(mainCls);
    ctx.getInputs().stream().map(OObject::getCls).forEach(clsKeys::add);
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
  public Uni<OObject> resolveObj(InvocationContext baseCtx, String ref) {
    if (ref.startsWith("$.")) {
      var refName = ref.substring(2);
      var refKey = baseCtx.getMain().getRefs().get(refName);
      if (refKey != null) {
        var mainCls = baseCtx.getMainCls();
        var refSpec = mainCls.findReference(ref.substring(2));
        var obj = baseCtx.getMainRefs().get(refName);
        if (obj!=null)
          return Uni.createFrom().item(obj);
        if (refSpec.isEmpty())
          throw FunctionValidationException.cannotResolveMacro(ref,
            "No ref to satisfy " + ref);
        return load(refSpec.get().getCls(), refKey)
          .onItem().ifNull()
          .failWith(() -> FunctionValidationException.cannotResolveMacro(ref, "object not found"))
          .invoke(o -> baseCtx.getMainRefs().put(refName, o));
      }
    } else {
      return Uni.createFrom().item(baseCtx.resolveDataflowRef(ref));
    }

    throw FunctionValidationException.cannotResolveMacro(ref, null);
  }

}
