package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ApplicationScoped
public class MacroFunctionHandler {

  @Inject
  MscObjectRepository objectRepo;
  @Inject
  TaskFunctionHandler taskFunctionHandler;
  @Inject
  ContextLoader contextLoader;

  public Uni<List<MscObject>> call(MscObject main,
                                   MscFunction function,
                                   Map<String, String> args) {
    if (main.getType()!=MscObject.Type.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (function.getType()!=MscFunction.Type.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);

    var templateObj = function.getOutputTemplate();
    templateObj.setOrigin(new MscObjectOrigin(main, function, args));

    return Uni.combine().all().unis(contextLoader.loadMembers(main), contextLoader.loadMembers(function))
      .combinedWith((members, macroMap) -> Multi.createFrom()
        .iterable(macroMap.entrySet())
        .onItem().transformToUni(entry -> {
          var mem = members.get(entry.getKey());
          var func = entry.getValue();
          return taskFunctionHandler.call(mem, func, args);
        }).concatenate())
      .onItem().transformToMulti(Function.identity())
      .flatMap(l -> Multi.createFrom().iterable(l))
      .collect().asList()
//      .map(l -> {
//        main.getMembers().entrySet().stream()
//          .map(e -> {
//
//          })
//      })
       ;
  }
}
