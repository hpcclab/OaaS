package org.hpcclab.oaas.crm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import org.hpcclab.oaas.crm.condition.Condition;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;
import java.util.Map;

@RegisterForReflection(ignoreNested = false)
@Builder(toBuilder = true)
public record CrtMappingConfig(
  Map<String, CrtConfig> templates
) {
  public interface ScalingConfig {
    int stabilizationWindow();

    double objectiveMissThreshold();

    int maxScaleStep();

    boolean disableDynamicAdjustment();
  }

  @Builder(toBuilder = true)
  public record CrtConfig(
    String type,
    Map<String, SvcConfig> services,
    FnConfig functions,
    String optimizer,
    Map<String, String> optimizerConf,
    Condition condition,
    int priority) {
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
    int maxScaleStep,
    int maxReplicas,
    int startReplicas,
    float startReplicasToTpRatio,
    boolean enableHpa,
    List<Toleration> tolerations,
    double objectiveMissThreshold,
    boolean disableDynamicAdjustment) implements ScalingConfig {
  }

  @Builder(toBuilder = true)
  public record FnConfig(
    int stabilizationWindow,
    int maxScaleStep,
    String defaultRequestCpu,
    String defaultRequestMem,
    String defaultScaleDawnDelay,
    int startReplicas,
    double objectiveMissThreshold,
    boolean disableDynamicAdjustment) implements ScalingConfig {
  }

  public record Toleration(
    String key,
    String value,
    String operator,
    String effect
  ) {
  }
}
