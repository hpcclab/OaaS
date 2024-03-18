package org.hpcclab.oaas.crm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import org.hpcclab.oaas.crm.condition.Condition;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.Map;

@RegisterForReflection(ignoreNested = false)
@Builder(toBuilder = true)
public record CrtMappingConfig(
  Map<String, CrtConfig> templates
) {
  public interface ScalingConfig {
    int stabilizationWindow();

    int maxScaleDiff();
  }

  @Builder(toBuilder = true)
  public record CrtConfig(
    String type,
    Map<String, SvcConfig> services,
    FnConfig functions,
    String optimizer,
    Map<String, String> optimizerConf,
    int stabilizationWindow,
    Condition condition,
    int priority
  ) {
  }

  @Builder(toBuilder = true)
  public record SvcConfig(
    String image,
    Map<String, String> env,
    String imagePullPolicy,
    CrInstanceSpec defaultSpec,
    String requestCpu,
    String requestMemory,
    String limitCpu,
    String limitMemory,
    int stabilizationWindow,
    int maxScaleDiff
  ) implements ScalingConfig {
  }

  @Builder(toBuilder = true)
  public record FnConfig(
    int stabilizationWindow,
    int maxScaleDiff,
    String defaultRequestCpu,
    String defaultRequestMem
  ) implements ScalingConfig {
  }
}
