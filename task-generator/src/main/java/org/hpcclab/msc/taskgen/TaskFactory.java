package org.hpcclab.msc.taskgen;

import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;

@ApplicationScoped
public class TaskFactory {

  public Task genTask(MscObject outputObj,
                      String requestFile,
                      FunctionExecContext context) {
    var function = context.getFunction();
    var mainObj = context.getTarget();
    var inputs = context.getAdditionalInputs();
    var template = function.getTask();
    var task = new Task();
    task.setFunctionName(function.getName());
    task.setMainObj(mainObj.getId().toString());
    task.setOutputObj(outputObj.getId().toString());
    task.setImage(template.getImage());
    task.setCommands(template.getCommands());
    task.setResourceType(outputObj.getState().getType().toString());
    task.setContainerArgs(template.getContainerArgs());
    if (function.getTask().isArgsToEnv() && mainObj.getOrigin().getArgs()!=null) {
      task.setEnv(mainObj.getOrigin().getArgs());
    } else {
      task.setEnv(new HashMap<>());
    }
    var env = task.getEnv();
    env.put("MAIN_ID", mainObj.getId().toString());
    env.put("MAIN_RESOURCE_URL", mainObj.getState().getBaseUrl());
    for (int i = 0; i < inputs.size(); i++) {
      MscObject inputObj = inputs.get(i);
      var prefix = "INPUT_" + i;
      env.put(prefix + "_ID", inputObj.getId().toString());
      env.put(prefix + "_RESOURCE_BASE_URL", inputObj.getState().getBaseUrl());
    }
    env.put("OUTPUT_RESOURCE_BASE_URL", outputObj.getState().getBaseUrl());
    env.put("REQUEST_FILE", requestFile);
    return task;
  }

  public TaskFlow genTaskSequence(MscObject outputObj,
                                  String requestFile,
                                  FunctionExecContext context) {
    var task = genTask(outputObj, requestFile, context);
    var seq = new TaskFlow()
      .setTask(task)
      .setId(Task.createId(outputObj, requestFile));
    var pre = new ArrayList<String>();
    pre.add(Task.createId(context.getTarget(), requestFile));
    for (MscObject additionalInput : context.getAdditionalInputs()) {
      pre.add(Task.createId(additionalInput, requestFile));
    }
    seq.setPrerequisiteTasks(pre);
    return seq;
  }
}
