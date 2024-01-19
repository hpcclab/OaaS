package org.hpcclab.oaas.orbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.hpcclab.oaas.proto.*;

import java.io.IOException;
import java.util.HashMap;

@ApplicationScoped
public class OrbitTemplateManager {
  ImmutableMap<String, OrbitTemplate> templateMap = Maps.immutable.empty();
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  KubernetesClient kubernetesClient;
  @GrpcClient("class-manager")
  OrbitStateUpdaterGrpc.OrbitStateUpdaterBlockingStub orbitStateUpdater;
  @GrpcClient("class-manager")
  OrbitStateServiceGrpc.OrbitStateServiceBlockingStub orbitStateService;

  public void load() {
    var file = "/orbit.yaml";
    var is = getClass().getResourceAsStream(file);
    try {
      var conf = yamlMapper.readValue(is, OrbitMappingConfig.class);
      if (conf.templates() == null || conf.templates().isEmpty()) {
        return;
      }
      var m = new HashMap<String, OrbitTemplate>();
      for (var configEntry : conf.templates().entrySet()) {
        var template = new DefaultOrbitTemplate(kubernetesClient, configEntry.getValue());
        m.put(configEntry.getKey(), template);
      }
      templateMap = Maps.immutable.ofMap(m);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public OrbitTemplate selectTemplate(DeploymentUnit deploymentUnit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }
  public OrbitTemplate selectTemplate(ProtoOrbit orbit) {
    // TODO PLACEHOLDER
    return templateMap.valuesView().getAny();
  }

  public OrbitStructure load(DeploymentUnit unit, ProtoOrbit orbit) {
    var temp = selectTemplate(orbit);
    return temp.load(orbit);
  }
}
