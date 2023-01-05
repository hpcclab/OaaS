package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.tuple.Pair;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectUpdate;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskDetail;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.EntityRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CompletionValidator {
  EntityRepository<String, OaasClass> clsRepo;

  @Inject
  public CompletionValidator(EntityRepository<String, OaasClass> clsRepo) {
    this.clsRepo = clsRepo;
  }

  public Uni<TaskCompletion> validate(TaskDetail taskDetail, TaskCompletion completion){
    Uni<Void> uni;
    if (completion.getMain() != null) {
      uni = validate(taskDetail.getMain().getCls(), completion.getMain());
    } else {
      uni = Uni.createFrom().voidItem();
    }
    if (completion.getOutput() != null) {
      uni = uni.flatMap(__ -> validate(taskDetail.getOutput().getCls(), completion.getOutput()));
    }

    return uni.replaceWith(completion);
  }

  private Uni<Void> validate(String clsKey, ObjectUpdate update) {
    return clsRepo.getAsync(clsKey)
      .invoke(update::filterKeys)
      .replaceWithVoid();
  }
}
