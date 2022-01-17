package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class TaskEvent {
  String id;
  Type type;
  Set<String> nextTasks;
  Set<String> prevTasks;
  Set<String> roots;
  String notifyFrom;
  int traverse = 0;
  boolean exec = true;

  public enum Type {
    CREATE, NOTIFY, COMPLETE
  }
}
