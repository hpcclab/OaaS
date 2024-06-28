package org.hpcclab.oaas.crm.filter;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class ConfigmapInjectingFilter implements CrFilter<List<HasMetadata>>{

  final String confName;

  public ConfigmapInjectingFilter(String confName) {
    this.confName = confName;
  }

  @Override
  public List<HasMetadata> applyOnCreate(List<HasMetadata> hasMetadataList) {
    for (var resource: hasMetadataList) {
      if (resource instanceof Deployment deployment) {
        List<Container> containers = deployment.getSpec()
          .getTemplate()
          .getSpec()
          .getContainers();
        injectConf(containers);
      }
    }
    return hasMetadataList;
  }

  @Override
  public List<HasMetadata> applyOnAdjust(List<HasMetadata> item) {
    return item;
  }

  private void injectConf(List<Container> containers) {
    for (Container container : containers) {
      container.getEnvFrom()
        .add(new EnvFromSource(
          new ConfigMapEnvSource(confName, false),
          null, null));
    }
  }


  @Override
  public String name() {
    return "ConfigmapInjecting";
  }
}
