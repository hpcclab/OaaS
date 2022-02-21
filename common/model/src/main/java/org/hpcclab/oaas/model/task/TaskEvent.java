package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.proto.TaskCompletion;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class TaskEvent {
  String id;
  Type type;
  Set<String> nextTasks;
  Set<String> prqTasks;
  String source;
  TaskCompletion completion;
  boolean exec = true;
  boolean entry = false;

  public enum Type {
    CREATE, NOTIFY, COMPLETE
  }
  public TaskEvent generatePrq(OaasObjectOrigin origin) {
    prqTasks = origin.getInputs().stream()
      .map(UUID::toString).collect(Collectors.toSet());
    if (origin.getParentId() != null)
      prqTasks.add(origin.getParentId().toString());
    return this;
  }
}
