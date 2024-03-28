package org.hpcclab.oaas.invocation.task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.task.OTask;

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

  public static InvokingDetail<OTask> of(OTask task, OFunction function) {
    return new InvokingDetail<>(
      task.getId(),
      task.getFuncKey(),
      function.getStatus().getInvocationUrl(),
      task,
      task.getTs()
    );
  }
}
