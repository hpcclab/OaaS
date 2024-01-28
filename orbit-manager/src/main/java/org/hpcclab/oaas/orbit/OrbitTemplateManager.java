package org.hpcclab.oaas.orbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.hpcclab.oaas.orbit.controller.OrbitController;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.optimize.DefaultQoSOptimizer;
import org.hpcclab.oaas.orbit.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.*;

import java.io.IOException;
import java.util.HashMap;

@ApplicationScoped
public class OrbitTemplateManager {
  ImmutableMap<String, OrbitTemplate> templateMap = Maps.immutable.empty();
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  KubernetesClient kubernetesClient;
  OrbitRepository orbitRepo;

  public OrbitTemplateManager(KubernetesClient kubernetesClient,
                              OrbitRepository orbitRepo) {
    this.kubernetesClient = kubernetesClient;
    this.orbitRepo = orbitRepo;
    loadTemplate();
  }

  public void loadTemplate() {
    var file = "/orbits.yaml";
    var is = getClass().getResourceAsStream(file);
    try {
      var conf = yamlMapper.readValue(is, OrbitMappingConfig.class);
      if (conf.templates() == null || conf.templates().isEmpty()) {
        return;
      }
      var m = new HashMap<String, OrbitTemplate>();
      for (var configEntry : conf.templates().entrySet()) {
        var template = new DefaultOrbitTemplate(
          kubernetesClient,
          selectOptimizer(configEntry.getValue()),
          configEntry.getValue()
        );
        m.put(configEntry.getKey(), template);
      }
      templateMap = Maps.immutable.ofMap(m);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public QosOptimizer selectOptimizer(OrbitMappingConfig.OrbitTemplateConfig config) {
    return new DefaultQoSOptimizer();
  }

  public OrbitTemplate selectTemplate(OprcEnvironment env,
                                      DeploymentUnit deploymentUnit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }
  public OrbitTemplate selectTemplate(ProtoOrbit orbit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }

  public OrbitController load(OprcEnvironment env, ProtoOrbit orbit) {
    return orbitRepo.getOrLoad(orbit.getId(),
      () -> selectTemplate(orbit).load(env, orbit)
    );
  }
}
