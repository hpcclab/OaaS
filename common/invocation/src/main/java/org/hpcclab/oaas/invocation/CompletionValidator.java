package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.EntityRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CompletionValidator {
  EntityRepository<String, OClass> clsRepo;
  EntityRepository<String, OFunction> funcRepo;

  @Inject
  public CompletionValidator(EntityRepository<String, OClass> clsRepo,
                             EntityRepository<String, OFunction> funcRepo) {
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

    return uni.replaceWith(completion);
  }

  private Uni<OOUpdate> validateUpdate(String clsKey,
                                       String fbName,
                                       boolean isMain,
                                       OOUpdate update) {
    return clsRepo.async().getAsync(clsKey)
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
}
