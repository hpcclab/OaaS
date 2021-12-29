package org.hpcclab.oaas.taskmanager.factory;

import org.hpcclab.oaas.model.task.OaasTask;
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
}
