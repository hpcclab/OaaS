package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContextLoader {
  @Inject
  MscObjectRepository objectRepo;
  @Inject
  MscFuncRepository funcRepo;


  public Uni<Map<String, MscObject>> loadMembers(MscObject main) {
    return objectRepo.listByIds(main.getMembers().values())
      .map(objList -> main.getMembers().entrySet()
        .stream()
        .map(e -> Map.entry(e.getKey(), objList.stream().filter(o -> o.getId().equals(e.getValue())).findFirst().orElseThrow()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
  }

  public Uni<Map<String, MscFunction>> loadMembers(MscFunction function) {
    return funcRepo.listByNames(function.getMacroMapping().values())
      .map(objList -> function.getMacroMapping().entrySet()
        .stream()
        .map(e -> Map.entry(e.getKey(), objList.stream().filter(o -> o.getName().equals(e.getValue())).findFirst().orElseThrow()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
  }
}
