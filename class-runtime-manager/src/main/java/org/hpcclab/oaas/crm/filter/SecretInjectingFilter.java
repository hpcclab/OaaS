package org.hpcclab.oaas.crm.filter;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class SecretInjectingFilter implements CrFilter<List<HasMetadata>>{

  final String secretName;

  public SecretInjectingFilter(String secretName) {
    this.secretName = secretName;
  }


  @Override
  public List<HasMetadata> applyOnCreate(List<HasMetadata> hasMetadataList) {
    for (var resource: hasMetadataList) {
      if (resource instanceof Deployment deployment) {
        List<Container> containers = deployment.getSpec()
          .getTemplate()
          .getSpec()
          .getContainers();
        injectSecret(containers);
      }
    }
    return hasMetadataList;
  }

  @Override
  public List<HasMetadata> applyOnAdjust(List<HasMetadata> item) {
    return item;
  }

  private void injectSecret(List<Container> containers) {
    for (Container container : containers) {
      container.getEnvFrom()
        .add(new EnvFromSource(null,
          null, new SecretEnvSource(secretName, false)));
    }
  }


  @Override
  public String name() {
    return "SecretInjecting";
  }
}
