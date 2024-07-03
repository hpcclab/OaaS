package org.hpcclab.oaas.crm.filter;

import io.fabric8.knative.serving.v1.RevisionSpec;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Pawissanutt
 */
public class PodAffinityInjectingFilter implements CrFilter<List<HasMetadata>> {

  final String key;
  final List<String> values;
  final boolean required;

  public PodAffinityInjectingFilter(String key, List<String> values) {
    this.key = key;
    this.values = values;
    this.required = true;
  }

  public PodAffinityInjectingFilter(Map<String,Object> conf) {
    Objects.requireNonNull(conf);
    this.key = (String) conf.get("key");
    this.values = List.of(conf.get("values").toString().split(","));
    this.required = (boolean) conf.getOrDefault("required", true);
  }

  @Override
  public List<HasMetadata> applyOnCreate(List<HasMetadata> hasMetadataList) {
    for (var resource : hasMetadataList) {
      if (resource instanceof Deployment deployment) {
        PodSpec spec = deployment.getSpec()
          .getTemplate()
          .getSpec();
        var affinity = spec.getAffinity();
        if (affinity == null) affinity = new Affinity();
        var newAffinity = injectAffinity(affinity);
        spec.setAffinity(newAffinity);
      } else if (resource instanceof Service service) {
        RevisionSpec spec = service.getSpec()
          .getTemplate()
          .getSpec();
        Affinity affinity = spec.getAffinity();
        if (affinity == null) affinity = new Affinity();
        var newAffinity = injectAffinity(affinity);
        spec.setAffinity(newAffinity);
      }
    }
    return hasMetadataList;
  }

  private Affinity injectAffinity(Affinity affinity) {
    PodAffinityTerm term = new PodAffinityTermBuilder()
      .withNewLabelSelector()
      .addNewMatchExpression()
      .withKey(key)
      .withOperator("In")
      .withValues(values)
      .endMatchExpression()
      .endLabelSelector()
      .withTopologyKey("kubernetes.io/hostname")
      .build();
    if (required) {
      return affinity.edit()
        .editPodAffinity()
        .addToRequiredDuringSchedulingIgnoredDuringExecution(term)
        .endPodAffinity()
        .build();
    } else {
      WeightedPodAffinityTerm wTerm = new WeightedPodAffinityTermBuilder()
        .withPodAffinityTerm(term)
        .withWeight(100)
        .build();
      return affinity.edit()
        .editPodAffinity()
        .addToPreferredDuringSchedulingIgnoredDuringExecution(wTerm)
        .endPodAffinity()
        .build();
    }
  }

  @Override
  public List<HasMetadata> applyOnAdjust(List<HasMetadata> item) {
    return item;
  }


  @Override
  public String name() {
    return NAME;
  }

  public static final String NAME = "PodAffinityInjecting";
}
