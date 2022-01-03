package org.hpcclab.oaas.taskmanager;

public class TaskEventException extends RuntimeException{
  public TaskEventException(Throwable cause) {
    super(cause);
  }

  public TaskEventException(String message) {
    super(message);
  }

  public static TaskEventException concurrentModification() {
    return new TaskEventException("Detech concurrent modification");
  }
}
