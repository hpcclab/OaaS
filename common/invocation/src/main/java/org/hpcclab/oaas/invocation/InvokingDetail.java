package org.hpcclab.oaas.invocation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.task.OaasTask;

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

  public static InvokingDetail<OaasTask> of(OaasTask task) {
    return new InvokingDetail<>(
      task.getId(),
      task.getFunction().getName(),
      task.getFunction().getDeploymentStatus().getInvocationUrl(),
      task,
      -1
    );
  }
}
