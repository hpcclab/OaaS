package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.CompletionCheckException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.repository.EntityRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CompletionValidator {
  EntityRepository<String, OaasClass> clsRepo;
  EntityRepository<String, OaasFunction> funcRepo;

  @Inject
  public CompletionValidator(EntityRepository<String, OaasClass> clsRepo,
                             EntityRepository<String, OaasFunction> funcRepo) {
    this.clsRepo = clsRepo;
    this.funcRepo = funcRepo;
  }

  public Uni<TaskCompletion> validateCompletion(InvocationContext context, TaskCompletion completion) {
    Uni<Void> uni;
    if (completion.getMain()!=null) {
      uni = validateUpdate(
        context.getMain().getCls(),
        context.getFbName(),
        true,
        completion.getMain())
        .invoke(completion::setMain)
        .replaceWithVoid();
    } else {
      uni = Uni.createFrom().voidItem();
    }
    if (context.getOutput()==null)
      completion.setOutput(null);
    else if (completion.getOutput()!=null) {
      uni = uni.flatMap(__ -> validateUpdate(
          context.getOutput().getCls(),
          context.getFbName(),
          false,
          completion.getOutput()))
        .invoke(completion::setOutput)
        .replaceWithVoid();
    }
    uni = uni.flatMap(__ -> validateFunction(context, completion));

    return uni.replaceWith(completion);
  }

  private Uni<ObjectUpdate> validateUpdate(String clsKey,
                                           String fbName,
                                           boolean isMain,
                                           ObjectUpdate update) {
    return clsRepo.getAsync(clsKey)
      .map(cls -> {
        if (isMain) {
          var fb = cls.findFunction(fbName);
          if (fb.isForceImmutable())
            return null;
        }
        update.filterKeys(cls);
        return update;
      });
  }

  private Uni<Void> validateFunction(TaskDetail taskDetail, TaskCompletion completion) {
    return funcRepo.getAsync(taskDetail.getFuncKey())
      .onItem()
      .ifNull().failWith(() -> new CompletionCheckException("Can not find the matched func"))
      .invoke(func -> {
        if (!func.getType().isMutable())
          completion.setMain(null);
      })
      .replaceWithVoid();
  }
}
