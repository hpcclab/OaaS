package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.orbit.OrbitMappingConfig.OrbitTemplateConfig;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOrbit;

public class DefaultOrbitTemplate implements OrbitTemplate {
  TsidFactory tsidFactory;
  KubernetesClient k8sClient;
  OrbitTemplateConfig config;
  public DefaultOrbitTemplate(KubernetesClient k8sClient,
                              OrbitTemplateConfig config) {
    this.k8sClient = k8sClient;
    this.config = config;
    tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public OrbitStructure create(OprcEnvironment env, DeploymentUnit deploymentUnit) {
    return new DeploymentOrbitStructure(this, k8sClient, env.config(), tsidFactory.create())
      .attach(deploymentUnit);
  }

  public OrbitStructure load(OprcEnvironment env, ProtoOrbit orbit) {
    return new DeploymentOrbitStructure(this, k8sClient, env.config(), orbit);
  }

  @Override
  public String type() {
    return "default";
  }

}
