package org.hpcclab.oaas.crm;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import org.hpcclab.oaas.crm.condition.Condition;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RegisterForReflection(ignoreNested = false)
@Builder(toBuilder = true)
public record CrtMappingConfig(
  Map<String, CrtConfig> templates
) {
  public interface ScalingConfig {
    int stabilizationWindow();

    double objectiveAmplifier();

    double objectiveMissThreshold();

    double idleFilterThreshold();

    int maxScaleStep();

    boolean disableDynamicAdjustment();
  }

  @Builder(toBuilder = true)
  public record CrtConfig(
    String type,
    Map<String, CrComponentConfig> services,
    FnConfig functions,
    String optimizer,
    Map<String, String> optimizerConf,
    Condition condition,
    int priority) {
  }

  @Builder(toBuilder = true)
  public record CrComponentConfig(
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
    List<FilterConfig> filters,
    double objectiveAmplifier,
    double objectiveMissThreshold,
    double idleFilterThreshold,
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
    double objectiveAmplifier,
    double objectiveMissThreshold,
    double idleFilterThreshold,
    List<FilterConfig> filters,
    boolean disableDynamicAdjustment) implements ScalingConfig {
  }



  public record FilterConfig(String type,
                             @JsonAnyGetter Map<String, Object> conf) {

    public FilterConfig {
      conf = conf==null ? new HashMap<>():conf;
    }

    @JsonAnySetter
    public void addAttribute(final String key, final Object value) {
      conf.put(key, value);
    }
  }
}
