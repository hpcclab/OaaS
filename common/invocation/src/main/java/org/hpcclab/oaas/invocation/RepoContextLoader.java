package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.invocation.InvocationNode;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.repository.EntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    uni = objectRepo.getAsync(request.target())
      .onItem().ifNull()
      .failWith(() -> StdOaasException.notFoundObject400(request.target()))
      .invoke(ctx::setMain);
    uni = uni.invoke(__ -> ctx.setEntry(ctx.getMain()))
      .map(ignore -> loadClsAndFunc(ctx, request.fb()))
      .flatMap(ignore -> objectRepo.orderedListAsync(request.inputs()))
      .invoke(ctx::setInputs);

    if (request.nodeExist()) {
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
    var mainCls = clsRepo.get(main.getCls());
    Set<String> clsKeys = Sets.mutable.of(main.getCls());

    if (mainCls==null)
      throw StdOaasException.format("Can not find class '%s'", main.getCls());
    ctx.setMainCls(mainCls);
    ctx.getInputs().stream().map(OaasObject::getCls).forEach(clsKeys::add);
    var clsMap = clsRepo.list(clsKeys);
    ctx.setClsMap(clsMap);

    var binding = mainCls
      .findFunction(fbName);
    if (binding==null)
      throw FunctionValidationException.noFunction(main.getId(), fbName);
    clsKeys.add(binding.getOutputCls());
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
  public Uni<OaasObject> resolveObj(InvocationContext baseCtx, String ref) {
//    logger.debug("resolve {}",ref);
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


//  public Uni<TaskContext> getTaskContextAsync(OaasObject output) {
//    var tc = new TaskContext();
//    var args = output.getOrigin().getArgs();
//    tc.setOutput(output)
//      .setArgs(KvPair.toMap(args));
//    var fbName = output.getOrigin().getFbName();
//    tc.setFbName(fbName);
//    var inputIds = output.getOrigin().getInputs();
//    var mainId = output.getOrigin().getParentId();
//    Uni<TaskContext> uni;
//    if (mainId==null) {
//      uni = Uni.createFrom().item(tc);
//    } else {
//      uni = objectRepo.getAsync(mainId)
//        .call(main -> loadRefs(main, tc))
//        .map(tc::setMain)
//        .call(__ -> loadFunction(tc, fbName));
//    }
//
//    if (!inputIds.isEmpty()) {
//      uni = uni.flatMap(ign -> objectRepo.orderedListAsync(inputIds)
//        .map(tc::setInputs));
//    }
//
//    return uni;
//  }

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

  public EntityRepository<String, InvocationNode> getInvNodeRepo() {
    return invNodeRepo;
  }
}
