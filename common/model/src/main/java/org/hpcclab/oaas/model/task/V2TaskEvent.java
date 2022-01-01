package org.hpcclab.oaas.model.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class V2TaskEvent {
  String id;
  Type type;
  Set<String> nextTasks;
  Set<String> prqTasks;
  String source;
  boolean exec = true;

  public enum Type {
    CREATE, NOTIFY, COMPLETE
  }
  public V2TaskEvent generatePrq(OaasObjectOrigin origin) {
    prqTasks = origin.getAdditionalInputs().stream()
      .map(UUID::toString).collect(Collectors.toSet());
    if (origin.getParentId() != null)
      prqTasks.add(origin.getParentId().toString());
    return this;
  }
}
