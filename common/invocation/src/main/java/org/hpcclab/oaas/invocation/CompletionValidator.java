package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.CompletionCheckException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.repository.EntityRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

  public Uni<TaskCompletion> validateUpdate(TaskDetail taskDetail, TaskCompletion completion){
    Uni<Void> uni;
    if (completion.getMain() != null) {
      uni = validateUpdate(taskDetail.getMain().getCls(), completion.getMain());
    } else {
      uni = Uni.createFrom().voidItem();
    }
    if (completion.getOutput() != null) {
      uni = uni.flatMap(__ -> validateUpdate(taskDetail.getOutput().getCls(), completion.getOutput()));
    }
    uni = uni.flatMap(__ -> validateFunction(taskDetail, completion));

    return uni.replaceWith(completion);
  }

  private Uni<Void> validateUpdate(String clsKey, ObjectUpdate update) {
    return clsRepo.getAsync(clsKey)
      .invoke(update::filterKeys)
      .replaceWithVoid();
  }

  private Uni<Void> validateFunction(TaskDetail taskDetail, TaskCompletion completion) {
    return funcRepo.getAsync(taskDetail.getFuncName())
      .onItem()
      .ifNull().failWith(() -> new CompletionCheckException("Can not find the matched function"))
      .invoke(func -> {
        if (!func.getType().isAllowUpdateMain())
          completion.setMain(null);
      })
      .replaceWithVoid();
  }
}
