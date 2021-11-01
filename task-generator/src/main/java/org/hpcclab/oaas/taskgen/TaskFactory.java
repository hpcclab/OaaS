package org.hpcclab.oaas.taskgen;

import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.model.TaskContext;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskFactory {

  public OaasTask genTask(TaskContext taskContext,
                          String requestFile) {
    var task = new OaasTask();
    task.setId(OaasTask.createId(taskContext.getOutput(), requestFile));
    task.setMain(taskContext.getParent());
    task.setOutput(taskContext.getOutput());
    task.setFunction(taskContext.getFunction());
    task.setAdditionalInputs(taskContext.getAdditionalInputs());
    return task;
  }

//  public TaskFlow genTaskSequence(OaasObject outputObj,
//                                  String requestFile,
//                                  FunctionExecContext context) {
//    var task = genTask(outputObj, requestFile, context);
//    var seq = new TaskFlow()
//      .setTask(task)
//      .setId(OaasTask.createId(outputObj, requestFile));
//    var pre = new HashSet<String>();
//    if (context.getMain().getOrigin().getParentId() != null) {
//      pre.add(OaasTask.createId(context.getMain(), requestFile));
//    }
//    for (OaasObject additionalInput : context.getAdditionalInputs()) {
//      if (additionalInput.getOrigin().getParentId() != null) {
//        pre.add(OaasTask.createId(additionalInput, requestFile));
//      }
//    }
////    seq.setPrerequisiteTasks(pre);
//    // TODO
//    return seq;
//  }
}
