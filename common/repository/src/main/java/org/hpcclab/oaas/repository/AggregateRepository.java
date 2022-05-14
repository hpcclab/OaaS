package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectReference;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
@Deprecated(forRemoval = true)
public class AggregateRepository {

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  OaasFuncRepository funcRepo;

  public TaskContext getTaskContext(String id) {
    var main = objectRepo.get(id);
    Objects.requireNonNull(main);

    var tc = new TaskContext();
    tc.setOutput(main);
    var funcName = main.getOrigin().getFuncName();
    var function = funcRepo.get(funcName);
    tc.setFunction(function);
    var inputs = objectRepo.listByIds(main.getOrigin().getInputs());
    tc.setInputs(inputs);
    if (main.getOrigin().getParentId()!=null) {
      var parent = objectRepo.get(main.getOrigin().getParentId());
      tc.setMain(parent);
    }
    if (main.getRefs() != null && !main.getRefs().isEmpty()) {
      var refSet = main.getRefs().stream().map(ObjectReference::getObjId)
        .collect(Collectors.toSet());
      var refMap = objectRepo.list(refSet);
      var mainRefs = main.getRefs().stream()
        .map(ref -> Map.entry(ref.getName(), refMap.get(ref.getObjId())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      tc.setMainRefs(mainRefs);
    }
    return tc;
  }

}
