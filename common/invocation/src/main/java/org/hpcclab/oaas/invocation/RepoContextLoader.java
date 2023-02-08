package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class RepoContextLoader implements ContextLoader {
  private static final Logger logger = LoggerFactory.getLogger(RepoContextLoader.class);

  EntityRepository<String, OaasObject> objectRepo;
  EntityRepository<String, OaasFunction> funcRepo;
  EntityRepository<String, OaasClass> clsRepo;

  @Inject
  public RepoContextLoader(EntityRepository<String, OaasObject> objectRepo,
                           EntityRepository<String, OaasFunction> funcRepo,
                           EntityRepository<String, OaasClass> clsRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
  }

  public Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLanguage request) {
    var ctx = new FunctionExecContext();
    ctx.setArgs(request.getArgs());
    return objectRepo.getAsync(request.getTarget())
      .onItem().ifNull()
      .failWith(() -> StdOaasException.notFoundObject400(request.getTarget()))
      .invoke(ctx::setMain)
      .invoke(ctx::setEntry)
      .map(ignore -> loadClsAndFunc(ctx, request.getFunctionName()))
      .flatMap(ignore -> objectRepo.orderedListAsync(request.getInputs()))
      .invoke(ctx::setInputs)
      .replaceWith(ctx);
  }

  @Override
  public Uni<FunctionExecContext> loadCtxAsync(InvocationRequest request) {
    var ctx = new FunctionExecContext();
    ctx.setArgs(request.args());
    ctx.setRequest(request);
    Uni<?> uni;
    if (request.loadOutput() && request.outId()!=null) {
      uni = objectRepo.listAsync(List.of(request.target(), request.outId()))
        .invoke(map -> ctx.setMain(map.get(request.target())).setOutput(map.get(request.outId())));
    } else {
      uni = objectRepo.getAsync(request.target())
        .onItem().ifNull()
        .failWith(() -> StdOaasException.notFoundObject400(request.target()))
        .invoke(ctx::setMain);
    }
    return uni.invoke(__ -> ctx.setEntry(ctx.getMain()))
      .map(ignore -> loadClsAndFunc(ctx, request.fbName()))
      .flatMap(ignore -> objectRepo.orderedListAsync(request.inputs()))
      .invoke(ctx::setInputs)
      .replaceWith(ctx);
  }

  public FunctionExecContext loadClsAndFunc(FunctionExecContext ctx, String fbName) {
    var main = ctx.getMain();
    var mainCls = clsRepo.get(main.getCls());
    if (mainCls==null)
      throw StdOaasException.format("Can not find class '%s'", main.getCls());
    ctx.setMainCls(mainCls);
    var binding = mainCls
      .findFunction(fbName);
    if (binding==null)
      throw FunctionValidationException.noFunction(main.getId(), fbName);
    ctx.setBinding(binding);

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
  public Uni<OaasObject> resolveObj(FunctionExecContext baseCtx, String ref) {
      if (ref.equals("$")) {
        return Uni.createFrom().item(baseCtx.getMain());
      }
      if (ref.startsWith("$.")) {
        var res = baseCtx.getMain().findReference(ref.substring(2));
        if (res.isPresent()) {
          var obj = baseCtx.getMainRefs().get(res.get().getName());
          if (obj!=null)
            return Uni.createFrom().item(obj);
          var id = res.get().getObjId();
          return objectRepo.getAsync(id)
            .invoke(o -> baseCtx.getMainRefs().put(id, o));
        }
      }
      if (ref.startsWith("#")) {
        try {
          var i = Integer.parseInt(ref.substring(1));
          if (i >= baseCtx.getInputs().size())
            throw FunctionValidationException.cannotResolveMacro(ref,
              "index out of range: >=" + baseCtx.getInputs().size());
          return Uni.createFrom().item(baseCtx.getInputs().get(i));
        } catch (NumberFormatException ignored) {
        }
      }

      if (baseCtx.getWorkflowMap().containsKey(ref))
        return Uni.createFrom().item(baseCtx.getWorkflowMap().get(ref));
      throw FunctionValidationException.cannotResolveMacro(ref, null);
    }
  //  public Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx,
//                                               DataflowStep step) {
//    var newCtx = new FunctionExecContext();
//    newCtx.setParent(baseCtx);
//    newCtx.setArgs(step.getArgs());
//    if (step.getArgRefs()!=null && !step.getArgRefs().isEmpty()) {
//      var map = new HashMap<String, String>();
//      Map<String, String> baseArgs = baseCtx.getArgs();
//      if (baseArgs!=null) {
//        for (var entry : step.getArgRefs().entrySet()) {
//          var resolveArg = baseArgs.get(entry.getValue());
//          map.put(entry.getKey(), resolveArg);
//        }
//      }
//      if (newCtx.getArgs()!=null) {
//        newCtx.setArgs(Maps.mutable.ofMap(newCtx.getArgs()));
//        newCtx.getArgs().putAll(map);
//      } else {
//        newCtx.setArgs(map);
//      }
//    }
//    baseCtx.addSubContext(newCtx);
//    return resolveObjFromCtx(baseCtx, step.getTarget())
//      .invoke(newCtx::setMain)
//      .map(ignore -> setClsAndFunc(newCtx, step.getFunction()))
//      .chain(() -> resolveInputs(newCtx, step));
//  }

//  private Uni<FunctionExecContext> resolveInputs(FunctionExecContext baseCtx,
//                                                 DataflowStep step) {
//    List<String> inputRefs = step.getInputRefs()==null ? List.of():step.getInputRefs();
//    return Multi.createFrom().iterable(inputRefs)
//      .onItem().transformToUniAndConcatenate(ref -> resolveObjFromCtx(baseCtx, ref))
//      .collect().asList()
//      .invoke(baseCtx::setInputs)
//      .replaceWith(baseCtx);
//  }
//
//  private Uni<OaasObject> resolveObjFromCtx(FunctionExecContext baseCtx, String ref) {
//    if (ref.equals("$")) {
//      return Uni.createFrom().item(baseCtx.getMain());
//    }
//    if (ref.startsWith("$.")) {
//      var res = baseCtx.getMain().findReference(ref.substring(2));
//      if (res.isPresent()) {
//        var obj = baseCtx.getMainRefs().get(res.get().getName());
//        if (obj!=null)
//          return Uni.createFrom().item(obj);
//        var id = res.get().getObjId();
//        return objectRepo.getAsync(id)
//          .invoke(o -> baseCtx.getMainRefs().put(id, o));
//      }
//    }
//    if (ref.startsWith("#")) {
//      try {
//        var i = Integer.parseInt(ref.substring(1));
//        if (i >= baseCtx.getInputs().size())
//          throw FunctionValidationException.cannotResolveMacro(ref,
//            "index out of range: >=" + baseCtx.getInputs().size());
//        return Uni.createFrom().item(baseCtx.getInputs().get(i));
//      } catch (NumberFormatException ignored) {
//      }
//    }
//
//    if (baseCtx.getWorkflowMap().containsKey(ref))
//      return Uni.createFrom().item(baseCtx.getWorkflowMap().get(ref));
//    throw FunctionValidationException.cannotResolveMacro(ref, null);
//  }

  public Uni<TaskContext> getTaskContextAsync(String outputId) {
    return objectRepo.getAsync(outputId)
      .flatMap(this::getTaskContextAsync);
  }

  public Uni<TaskContext> getTaskContextAsync(OaasObject output) {
    var tc = new TaskContext();
    tc.setOutput(output)
      .setArgs(output.getOrigin().getArgs());
    var fbName = output.getOrigin().getFbName();
    tc.setFbName(fbName);
    var inputIds = output.getOrigin().getInputs();
    var mainId = output.getOrigin().getParentId();
    Uni<TaskContext> uni;
    if (mainId==null) {
      uni = Uni.createFrom().item(tc);
    } else {
      uni = objectRepo.getAsync(mainId)
        .call(main -> loadRefs(main, tc))
        .map(tc::setMain)
        .call(__ -> loadFunction(tc, fbName));
    }

    if (!inputIds.isEmpty()) {
      uni = uni.flatMap(ign -> objectRepo.orderedListAsync(inputIds)
        .map(tc::setInputs));
    }

    return uni;
  }

  private Uni<TaskContext> loadRefs(OaasObject main, TaskContext ctx) {
    if (main.getRefs()!=null && !main.getRefs().isEmpty()) {
      var refSet = main.getRefs().stream().map(ObjectReference::getObjId)
        .collect(Collectors.toSet());
      return objectRepo.listAsync(refSet)
        .map(refMap -> {
          var mainRefs = main.getRefs().stream()
            .map(ref -> Map.entry(ref.getName(), refMap.get(ref.getObjId())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
          return ctx.setMainRefs(mainRefs);
        });
    }
    return Uni.createFrom().nullItem();
  }

  private Uni<?> loadFunction(TaskContext ctx, String fbName) {
    return clsRepo.getAsync(ctx.getMain().getCls())
      .call(cls -> {
        var fb = cls.findFunction(fbName);
        var funcKey = fb.getFunction();
        return funcRepo.getAsync(funcKey)
          .invoke(function -> ctx.setFunction(function)
            .setImmutable(fb.isForceImmutable() || !function.getType().isMutable()));
      });
  }

  public EntityRepository<String, OaasObject> getObjectRepo() {
    return objectRepo;
  }

  public EntityRepository<String, OaasFunction> getFuncRepo() {
    return funcRepo;
  }

  public EntityRepository<String, OaasClass> getClsRepo() {
    return clsRepo;
  }
}
