package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.exception.NoStackException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class AggregateRepository {
  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository clsRepo;

  public TaskContext getTaskContext(UUID id) {
    var main = objectRepo.get(id);
    var tc = new TaskContext();
    tc.setOutput(main);
    var funcName = main.getOrigin().getFuncName();
    var function = funcRepo.get(funcName);
    tc.setFunction(function);
    var inputs = objectRepo.listByIds(main.getOrigin().getAdditionalInputs());
    tc.setAdditionalInputs(inputs);
    if (main.getOrigin().getParentId()!=null) {
      var parent = objectRepo.get(main.getOrigin().getParentId());
      tc.setParent(parent);
    }
    return tc;
  }

  public Uni<TaskContext> getTaskContextAsync(UUID id) {
    var tc = new TaskContext();
    return objectRepo.getAsync(id)
      .onItem().ifNull().failWith(() -> NoStackException.notFoundObject400(id))
      .flatMap(main -> {
        tc.setOutput(main);
        return funcRepo.getAsync(main.getOrigin().getFuncName());
      })
      .flatMap(func -> {
        tc.setFunction(func);
        return objectRepo.listByIdsAsync(tc.getOutput().getOrigin().getAdditionalInputs());
      })
      .flatMap(inputs -> {
        tc.setAdditionalInputs(inputs);
        var parentId = tc.getOutput().getOrigin().getParentId();
        if (parentId != null){
          return objectRepo.getAsync(parentId)
            .map(tc::setParent);
        }
        return Uni.createFrom().item(tc);
      });
  }
}
