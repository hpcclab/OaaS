package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.task.BaseTaskMessage;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class TaskEvent extends BaseTaskMessage {
  Type type;
  Set<String> nextTasks;
  Set<String> prevTasks;
  Set<String> roots;
  String notifyFrom;
  int traverse = 0;
  boolean exec = true;

  @Override
  public TaskEvent setId(String id) {
    this.id = id;
    return this;
  }

  public enum Type {
    CREATE, NOTIFY, COMPLETE
  }
}
