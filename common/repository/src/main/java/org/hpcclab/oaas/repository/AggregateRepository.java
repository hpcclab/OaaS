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
    tc.setOutputCls(clsRepo.get(main.getCls()));
    var funcName = main.getOrigin().getFuncName();
    var function = funcRepo.get(funcName);
    tc.setFunction(function);
    var inputs = objectRepo.listByIds(main.getOrigin().getInputs());
    tc.setInputs(inputs);
    var inputCls = inputs.stream()
      .map(input -> clsRepo.get(input.getCls()))
      .toList();
    tc.setInputCls(inputCls);
    if (main.getOrigin().getParentId()!=null) {
      var parent = objectRepo.get(main.getOrigin().getParentId());
      tc.setMain(parent);
      tc.setMainCls(clsRepo.get(parent.getCls()));
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
      .invoke(() -> tc.setOutputCls(clsRepo.get(tc.getOutput().getCls())))
      .flatMap(func -> {
        tc.setFunction(func);
        return objectRepo.listByIdsAsync(tc.getOutput().getOrigin().getInputs());
      })
      .flatMap(inputs -> {
        tc.setInputs(inputs);
        var inputCls = inputs.stream()
          .map(input -> clsRepo.get(input.getCls()))
          .toList();
        tc.setInputCls(inputCls);
        var parentId = tc.getOutput().getOrigin().getParentId();
        if (parentId != null){
          return objectRepo.getAsync(parentId)
            .map(tc::setMain)
            .invoke(() -> tc.setOutputCls(clsRepo.get(tc.getMain().getCls())));
        }
        return Uni.createFrom().item(tc);
      });
  }
}
