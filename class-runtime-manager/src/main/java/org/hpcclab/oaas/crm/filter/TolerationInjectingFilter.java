package org.hpcclab.oaas.crm.filter;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class TolerationInjectingFilter implements CrFilter<List<HasMetadata>> {

  public static final String NAME = "TolerationInjecting";
  final ParsedToleration toleration;

  public TolerationInjectingFilter(Map<String, Object> conf) {
    toleration = new ParsedToleration(
      (String) conf.get("key"),
      (String) conf.get("value"),
      (String) conf.getOrDefault("operator", "Equal"),
      (String) conf.getOrDefault("effect", "NoSchedule")
    );
  }

  @Override
  public List<HasMetadata> applyOnCreate(List<HasMetadata> hasMetadataList) {
    for (var resource : hasMetadataList) {
      if (resource instanceof Deployment deployment) {
        PodSpec podSpec = deployment.getSpec()
          .getTemplate()
          .getSpec();
        injectToleration(podSpec);
      }
    }
    return hasMetadataList;
  }

  private void injectToleration(PodSpec podSpec) {
    podSpec
      .getTolerations()
      .add(new Toleration(
        toleration.effect(),
        toleration.key(),
        toleration.operator(),
        null,
        toleration.value())
      );
  }

  @Override
  public List<HasMetadata> applyOnAdjust(List<HasMetadata> item) {
    return item;
  }

  @Override
  public String name() {
    return NAME;
  }

  public record ParsedToleration(
    String key,
    String value,
    String operator,
    String effect
  ) {
  }
}
