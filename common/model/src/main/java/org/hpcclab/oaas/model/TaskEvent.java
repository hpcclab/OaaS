package org.hpcclab.oaas.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
public class TaskEvent {
  Type type;
  String id;
  Set<String> nextTasks;
  Set<String> prevTasks;
  String notifyFrom;
  int traverse = 0;


  public enum Type {
    CREATE, EXEC, NOTIFY, COMPLETE
  }
}
