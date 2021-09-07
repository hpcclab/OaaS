package org.hpcclab.msc.taskgen;

import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.ObjectStateRequest;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskGenerator {

  public Task genTask(ObjectStateRequest request,
                      MscObject object,
                      MscFunction function) {
    return new Task();
  }

}
