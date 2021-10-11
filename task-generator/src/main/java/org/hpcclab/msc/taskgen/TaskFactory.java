package org.hpcclab.msc.taskgen;

import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.entity.task.Task;
import org.hpcclab.msc.object.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;

@ApplicationScoped
public class TaskFactory {

  public Task genTask(OaasObject outputObj,
                      String requestFile,
                      FunctionExecContext context) {
    var function = context.getFunction();
    var mainObj = context.getMain();
    var inputs = context.getAdditionalInputs();
    var task = new Task();
    task.setId(Task.createId(outputObj, requestFile));
    task.setMain(mainObj);
    task.setOutput(outputObj);
    task.setFunction(function);
    task.setAdditionalInputs(inputs);
    task.setArgs(context.getArgs());
    return task;
  }

  public TaskFlow genTaskSequence(OaasObject outputObj,
                                  String requestFile,
                                  FunctionExecContext context) {
    var task = genTask(outputObj, requestFile, context);
    var seq = new TaskFlow()
      .setTask(task)
      .setId(Task.createId(outputObj, requestFile));
    var pre = new ArrayList<String>();
    if (context.getMain().getOrigin().getParentId() != null) {
      pre.add(Task.createId(context.getMain(), requestFile));
    }
    for (OaasObject additionalInput : context.getAdditionalInputs()) {
      if (additionalInput.getOrigin().getParentId() != null) {
        pre.add(Task.createId(additionalInput, requestFile));
      }
    }
    seq.setPrerequisiteTasks(pre);
    return seq;
  }
}
