package org.hpcclab.oaas.invocation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OFunctionDeploymentStatus;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.task.OTask;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class InvokingDetail<V> {
  String id;
  String funcName;
  String funcUrl;
  V content;
  long smtTs = -1;

  public static InvokingDetail<OTask> of(OTask task) {
    return new InvokingDetail<>(
      task.getId(),
      task.getFuncKey(),
      Optional.of(task.getFunction())
        .map(OFunction::getStatus)
        .map(OFunctionDeploymentStatus::getInvocationUrl)
        .orElse(null),
      task,
      task.getTs()
    );
  }
}
