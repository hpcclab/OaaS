package org.hpcclab.oaas.crm.filter;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.crm.controller.K8sResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class PodMonitorInjectingFilter implements CrFilter<List<HasMetadata>>{

  final KubernetesClient k8sClient;

  public PodMonitorInjectingFilter(KubernetesClient k8sClient) {
    this.k8sClient = k8sClient;
  }

  @Override
  public List<HasMetadata> applyOnCreate(List<HasMetadata> hasMetadataList) {
    var newRes = Lists.mutable.ofAll(hasMetadataList);
    for (var resource: hasMetadataList) {
      if (resource instanceof Deployment deployment) {
        ObjectMeta metadata = deployment.getMetadata();
        var podMonitor = K8sResourceUtil
          .createPodMonitor(metadata.getName(), metadata.getNamespace(),
            metadata.getLabels());
        newRes.add(podMonitor);
      }
    }

    return newRes;
  }

  @Override
  public List<HasMetadata> applyOnAdjust(List<HasMetadata> item) {
    return item;
  }

  private static final Logger logger = LoggerFactory.getLogger( PodMonitorInjectingFilter.class );
  @Override
  public List<HasMetadata> applyOnDelete(List<HasMetadata> item) {
    var newRes = Lists.mutable.ofAll(item);
    for (var resource: item) {
      if (resource instanceof Deployment deployment) {
        ObjectMeta metadata = deployment.getMetadata();
        var podMonitor = k8sClient
          .genericKubernetesResources("monitoring.coreos.com/v1", "PodMonitor")
          .withLabels(metadata.getLabels())
          .list()
          .getItems();
        logger.debug("delete podmonitor {}", podMonitor);
        newRes.addAll(podMonitor);
      }
    }
    return newRes;
  }

  @Override
  public String name() {
    return "PodMonitorInjecting";
  }
}
