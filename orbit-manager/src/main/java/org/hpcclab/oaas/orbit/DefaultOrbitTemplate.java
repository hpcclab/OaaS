package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.orbit.OrbitMappingConfig.OrbitTemplateConfig;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;
import org.hpcclab.oaas.orbit.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoOrbit;

public class DefaultOrbitTemplate implements OrbitTemplate {
  TsidFactory tsidFactory;
  KubernetesClient k8sClient;
  OrbitTemplateConfig config;
  QosOptimizer qosOptimizer;
  public DefaultOrbitTemplate(KubernetesClient k8sClient,
                              QosOptimizer qosOptimizer,
                              OrbitTemplateConfig config) {
    this.k8sClient = k8sClient;
    this.config = config;
    this.qosOptimizer = qosOptimizer;
    tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public OrbitStructure create(OprcEnvironment env, DeploymentUnit deploymentUnit) {
    return new DeploymentOrbitStructure(
      this, k8sClient, env.config(), tsidFactory.create()
    );
  }

  public OrbitStructure load(OprcEnvironment env, ProtoOrbit orbit) {
    return new DeploymentOrbitStructure(this, k8sClient, env.config(), orbit);
  }

  @Override
  public String type() {
    return "default";
  }

  @Override
  public QosOptimizer getQosOptimizer() {
    return qosOptimizer;
  }
}
