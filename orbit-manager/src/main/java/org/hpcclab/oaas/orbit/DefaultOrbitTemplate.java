package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.orbit.OrbitMappingConfig.OrbitTemplateConfig;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOrbit;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;

import java.util.List;
import java.util.Map;

public class DefaultOrbitTemplate implements OrbitTemplate {
  TsidFactory tsidFactory;
  KubernetesClient k8sClient;
  OrbitTemplateConfig config;
  public DefaultOrbitTemplate(KubernetesClient k8sClient, OrbitTemplateConfig config) {
    this.k8sClient = k8sClient;
    this.config = config;
    tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public OrbitStructure create(DeploymentUnit deploymentUnit) {
    return new DeploymentOrbitStructure(this, k8sClient, tsidFactory.create())
      .attach(deploymentUnit);
  }

  public OrbitStructure load(ProtoOrbit orbit) {
    return new DeploymentOrbitStructure(this, k8sClient, orbit);
  }

  @Override
  public String type() {
    return "default";
  }

}
