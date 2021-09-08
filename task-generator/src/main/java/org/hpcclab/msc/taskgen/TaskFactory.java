package org.hpcclab.msc.taskgen;

import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.ObjectStateRequest;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;

@ApplicationScoped
public class TaskFactory {

  public Task genTask(ObjectStateRequest request,
                      MscObject object,
                      List<MscObject> inputs,
                      MscFunction function) {
    var template = function.getTask();
    var task = new Task();
    task.setFunctionName(function.getName());
    task.setImage(template.getImage());
    task.setCommands(template.getCommands());
    task.setContainerArgs(template.getContainerArgs());
    if (function.getTask().isArgsToEnv() && object.getOrigin().getArgs()!= null) {
      task.setEnv(object.getOrigin().getArgs());
    } else {
      task.setEnv(new HashMap<>());
    }
    var env = task.getEnv();
    env.put("MAIN_ID", object.getId().toString());
    env.put("MAIN_STATE_URL", object.getState().getUrl());
    for (int i = 0; i < inputs.size(); i++) {
      MscObject inputObj = inputs.get(i);
      var prefix = "INPUT_"+i;
      env.put(prefix+"_ID", inputObj.getId().toString());
      env.put(prefix+"_STATE_URL", inputObj.getState().getUrl());
    }
    env.put("REQUEST_FILE", request.getRequestFile());
    return task;
  }

}
