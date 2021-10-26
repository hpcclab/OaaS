package org.hpcclab.msc.taskgen;

import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.task.TaskFlow;
import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.model.FunctionExecContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashSet;

@ApplicationScoped
public class TaskFactory {

  public OaasTask genTask(OaasObject outputObj,
                          String requestFile,
                          FunctionExecContext context) {
    var function = context.getFunction();
    var mainObj = context.getMain();
    var inputs = context.getAdditionalInputs();
    var task = new OaasTask();
    task.setId(OaasTask.createId(outputObj, requestFile));
//    task.setMain(mainObj);
//    task.setOutput(outputObj);
//    task.setFunction(function);
//    task.setAdditionalInputs(inputs);
    return task;
  }

  public TaskFlow genTaskSequence(OaasObject outputObj,
                                  String requestFile,
                                  FunctionExecContext context) {
    var task = genTask(outputObj, requestFile, context);
    var seq = new TaskFlow()
      .setTask(task)
      .setId(OaasTask.createId(outputObj, requestFile));
    var pre = new HashSet<String>();
    if (context.getMain().getOrigin().getParentId() != null) {
      pre.add(OaasTask.createId(context.getMain(), requestFile));
    }
    for (OaasObject additionalInput : context.getAdditionalInputs()) {
      if (additionalInput.getOrigin().getParentId() != null) {
        pre.add(OaasTask.createId(additionalInput, requestFile));
      }
    }
//    seq.setPrerequisiteTasks(pre);
    // TODO
    return seq;
  }
}
