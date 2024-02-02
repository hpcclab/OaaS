package org.hpcclab.oaas.crm.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.hpcclab.oaas.crm.CrRepository;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.DefaultQoSOptimizer;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.*;

import java.io.IOException;
import java.util.HashMap;

@ApplicationScoped
public class CrTemplateManager {
  ImmutableMap<String, ClassRuntimeTemplate> templateMap = Maps.immutable.empty();
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  KubernetesClient kubernetesClient;
  CrRepository orbitRepo;

  public CrTemplateManager(KubernetesClient kubernetesClient,
                           CrRepository orbitRepo) {
    this.kubernetesClient = kubernetesClient;
    this.orbitRepo = orbitRepo;
    loadTemplate();
  }

  public void loadTemplate() {
    var file = "/crts.yaml";
    var is = getClass().getResourceAsStream(file);
    try {
      var conf = yamlMapper.readValue(is, CrtMappingConfig.class);
      if (conf.templates() == null || conf.templates().isEmpty()) {
        return;
      }
      var m = new HashMap<String, ClassRuntimeTemplate>();
      for (var configEntry : conf.templates().entrySet()) {
        var template = createCrt(configEntry.getValue());
        m.put(configEntry.getKey(), template);
      }
      templateMap = Maps.immutable.ofMap(m);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ClassRuntimeTemplate createCrt(CrtMappingConfig.CrtConfig config){
    if (config.type().equals("default")) {
      return new DefaultCrTemplate(
        kubernetesClient,
        selectOptimizer(config),
        config
      );
    } else {
      throw new RuntimeException("No available CR template with type " + config.type());
    }
  }

  public QosOptimizer selectOptimizer(CrtMappingConfig.CrtConfig config) {
    return new DefaultQoSOptimizer();
  }

  public ClassRuntimeTemplate selectTemplate(OprcEnvironment env,
                                             DeploymentUnit deploymentUnit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }
  public ClassRuntimeTemplate selectTemplate(ProtoCr orbit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }

  public CrController load(OprcEnvironment env, ProtoCr orbit) {
    return orbitRepo.getOrLoad(orbit.getId(),
      () -> selectTemplate(orbit).load(env, orbit)
    );
  }
}
