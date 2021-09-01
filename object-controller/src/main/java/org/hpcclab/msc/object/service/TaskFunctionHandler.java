package org.hpcclab.msc.object.service;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TaskFunctionHandler {
  public Uni<List<MscObject>> call(MscObject main,
                                   MscFunction function,
                                   Map<String, String> args) {
    return null;
  }
}
