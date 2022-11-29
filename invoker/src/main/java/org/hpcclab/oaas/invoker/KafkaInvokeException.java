package org.hpcclab.oaas.invoker;

import org.hpcclab.oaas.model.task.TaskCompletion;

public class KafkaInvokeException extends RuntimeException{
  TaskCompletion taskCompletion;

  public KafkaInvokeException(String message, TaskCompletion taskCompletion) {
    super(message);
    this.taskCompletion = taskCompletion;
  }

  public KafkaInvokeException(String message, Throwable cause, TaskCompletion taskCompletion) {
    super(message, cause);
    this.taskCompletion = taskCompletion;
  }

  public KafkaInvokeException(TaskCompletion taskCompletion) {
    this.taskCompletion = taskCompletion;
  }

  public KafkaInvokeException withTaskCompletion(TaskCompletion taskCompletion) {
    this.taskCompletion = taskCompletion;
    return this;
  }

  public KafkaInvokeException(Throwable cause, TaskCompletion taskCompletion) {
    super(cause);
    this.taskCompletion = taskCompletion;
  }

  public TaskCompletion getTaskCompletion() {
    return taskCompletion;
  }
}
