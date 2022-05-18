package org.hpcclab.oaas.repository.function;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasDataflowStep;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLangauge;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.impl.OaasClassRepository;
import org.hpcclab.oaas.repository.impl.OaasFuncRepository;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class RepoContextLoader implements ContextLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(RepoContextLoader.class);
  EntityRepository<String, OaasObject> objectRepo;
  EntityRepository<String, OaasFunction> funcRepo;
  EntityRepository<String, OaasClass> clsRepo;

  @Inject
  public RepoContextLoader(OaasObjectRepository objectRepo,
                           OaasFuncRepository funcRepo,
                           OaasClassRepository clsRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
  }

  public RepoContextLoader(EntityRepository<String, OaasObject> objectRepo,
                           EntityRepository<String, OaasFunction> funcRepo,
                           EntityRepository<String, OaasClass> clsRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
  }

  @Override
  public Uni<OaasObject> getObject(String id) {
    return objectRepo.getAsync(id);
  }

  public Uni<FunctionExecContext> loadCtxAsync(ObjectAccessLangauge request) {
    var ctx = new FunctionExecContext()
      .setArgs(request.getArgs());
    return objectRepo.getAsync(request.getTarget())
      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(request.getTarget()))
      .invoke(ctx::setMain)
      .invoke(ctx::setEntry)
      .map(ignore -> setClsAndFunc(ctx, request.getFunctionName()))
      .flatMap(ignore -> objectRepo.orderedListAsync(request.getInputs()))
      .invoke(ctx::setInputs)
      .replaceWith(ctx);
  }

  private FunctionExecContext setClsAndFunc(FunctionExecContext ctx, String funcName) {
    var main = ctx.getMain();
    var mainCls = clsRepo.get(main.getCls());
    ctx.setMainCls(mainCls);
    var binding = mainCls
      .findFunction(funcName);
    if (binding.isEmpty()) throw FunctionValidationException.noFunction(main.getId(), funcName);
    ctx.setBinding(binding.get());

    var func = funcRepo.get(binding.get().getFunction());
    if (func==null)
      throw NoStackException.notFoundFunc400(funcName);
    ctx.setFunction(func);
    if (func.getOutputCls()!=null) {
      var outputClass = clsRepo.get(func.getOutputCls());
      ctx.setOutputCls(outputClass);
    }
    return ctx;
  }

  public Uni<FunctionExecContext> loadCtxAsync(FunctionExecContext baseCtx,
                                               OaasDataflowStep step) {
    var newCtx = new FunctionExecContext();
    newCtx.setParent(baseCtx);
    newCtx.setArgs(step.getArgs());
    if (step.getArgRefs()!=null && !step.getArgRefs().isEmpty()) {
      var map = new HashMap<String, String>();
      for (var entry : step.getArgRefs().entrySet()) {
        var resolveArg = baseCtx.getArgs().get(entry.getValue());
        if (resolveArg==null) throw new FunctionValidationException(
          "Can not resolve args '%s' from step %s".formatted(entry.getValue(), step));
        map.put(entry.getKey(), resolveArg);
      }
      if (newCtx.getArgs()!=null)
        newCtx.getArgs().putAll(map);
      else
        newCtx.setArgs(map);
    }
    baseCtx.addSubContext(newCtx);
    return resolveTarget(baseCtx, step.getTarget())
      .invoke(newCtx::setMain)
//      .flatMap(ignore -> setClsAndFuncAsync(newCtx, step.getFuncName()))
      .map(ignore -> setClsAndFunc(newCtx, step.getFuncName()))
      .chain(() -> resolveInputs(newCtx, step));
  }

  private Uni<FunctionExecContext> resolveInputs(FunctionExecContext baseCtx,
                                                 OaasDataflowStep step) {
    List<String> inputRefs = step.getInputRefs()==null ? List.of():step.getInputRefs();
    return Multi.createFrom().iterable(inputRefs)
      .onItem().transformToUniAndConcatenate(ref -> resolveTarget(baseCtx, ref))
      .collect().asList()
      .invoke(baseCtx::setInputs)
      .replaceWith(baseCtx);
  }

  private Uni<OaasObject> resolveTarget(FunctionExecContext baseCtx, String ref) {
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
    if (baseCtx.getWorkflowMap().containsKey(ref))
      return Uni.createFrom().item(baseCtx.getWorkflowMap().get(ref));
    throw new NoStackException("Can not resolve '" + ref + "'");
  }


  public Uni<TaskContext> getTaskContextAsync(OaasObject output) {
    var tc = new TaskContext();
    tc.setOutput(output);
    var funcName = output.getOrigin().getFuncName();
    var func = funcRepo.get(funcName);
    tc.setFunction(func);
    var inputIds = output.getOrigin().getInputs();
    var mainId = output.getOrigin().getParentId();
    Uni<TaskContext> uni;
    if (mainId==null) {
      uni = Uni.createFrom().item(tc);
    } else {
      uni = objectRepo.getAsync(mainId)
        .call(main -> {
          if (main.getRefs()!=null && !main.getRefs().isEmpty()) {
            var refSet = main.getRefs().stream().map(ObjectReference::getObjId)
              .collect(Collectors.toSet());
            return objectRepo.listAsync(refSet)
              .map(refMap -> {
                var mainRefs = main.getRefs().stream()
                  .map(ref -> Map.entry(ref.getName(), refMap.get(ref.getObjId())))
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                return tc.setMainRefs(mainRefs);
              });
          }
          return Uni.createFrom().nullItem();
        })
        .map(tc::setMain);

    }

    if (!inputIds.isEmpty()) {
      uni = uni.flatMap(ign -> objectRepo.orderedListAsync(inputIds)
        .map(tc::setInputs));
    }

    return uni;
  }
}
