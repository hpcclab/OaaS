package org.hpcclab.msc.taskgen;

import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.List;

@ApplicationScoped
public class TaskFactory {

  public Task genTask(ObjectResourceRequest request,
                      FunctionExecContext context) {
    var function = context.getFunction();
    var mainObj = context.getTarget();
    var outputObj = context.getTarget();
    var inputs = context.getAdditionalInputs();
    var template = function.getTask();
    var task = new Task();
    task.setFunctionName(function.getName());
    task.setMainObj(mainObj.getId().toString());
    task.setOutputObj(request.getOwnerObjectId());
    task.setImage(template.getImage());
    task.setCommands(template.getCommands());
    task.setResourceType(outputObj.getState().getType().toString());
    task.setContainerArgs(template.getContainerArgs());
    if (function.getTask().isArgsToEnv() && mainObj.getOrigin().getArgs()!= null) {
      task.setEnv(mainObj.getOrigin().getArgs());
    } else {
      task.setEnv(new HashMap<>());
    }
    var env = task.getEnv();
    env.put("MAIN_ID", mainObj.getId().toString());
    env.put("MAIN_RESOURCE_URL", mainObj.getState().getUrl());
    for (int i = 0; i < inputs.size(); i++) {
      MscObject inputObj = inputs.get(i);
      var prefix = "INPUT_"+i;
      env.put(prefix+"_ID", inputObj.getId().toString());
      env.put(prefix+"_RESOURCE_URL", inputObj.getState().getUrl());
    }
    env.put("OUTPUT_RESOURCE_URL", outputObj.getState().getUrl());
    env.put("REQUEST_FILE", request.getRequestFile());
    return task;
  }

}
