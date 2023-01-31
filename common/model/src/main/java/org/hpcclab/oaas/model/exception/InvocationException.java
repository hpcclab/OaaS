package org.hpcclab.oaas.model.exception;

import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.task.TaskCompletion;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvocationException extends StdOaasException {

  TaskCompletion taskCompletion;
  public InvocationException(String message, Throwable cause) {
    super(message, cause, true, 500);
  }

  public InvocationException(String message) {
    super(message, null, true, 500);
  }

  public InvocationException(String message, int code) {
    super(message, code);
  }

  public InvocationException(String message, Throwable cause, int code) {
    super(message, cause, true, code);
  }

  public InvocationException(Throwable cause, TaskCompletion taskCompletion) {
    super(null, cause);
    this.taskCompletion = taskCompletion;
  }

  public static InvocationException detectConcurrent(Throwable e) {
    return new InvocationException("Detect concurrent update in the same object", e, 409);
  }

  public static InvocationException notReady(List<Map.Entry<OaasObject, OaasObject>> waiting,
                                             List<OaasObject> failed) {
    return new InvocationException("Dependencies are not ready. {waiting:[%s], failed:[%s]}"
      .formatted(
        waiting.stream()
          .map(entry -> entry.getKey().getId() + ">>" + entry.getValue().getId())
          .collect(Collectors.joining(", ")),
        failed.stream()
          .map(OaasObject::getId)
          .collect(Collectors.joining(", "))),
      409);
  }

  public TaskCompletion getTaskCompletion() {
    return taskCompletion;
  }

  public InvocationException setTaskCompletion(TaskCompletion taskCompletion) {
    this.taskCompletion = taskCompletion;
    return this;
  }
}
