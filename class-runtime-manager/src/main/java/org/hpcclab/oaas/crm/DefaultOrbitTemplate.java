package org.hpcclab.oaas.crm;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrtMappingConfig.CrtConfig;
import org.hpcclab.oaas.crm.controller.DeploymentCrController;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public class DefaultOrbitTemplate implements OrbitTemplate {
  TsidFactory tsidFactory;
  KubernetesClient k8sClient;
  CrtConfig config;
  QosOptimizer qosOptimizer;
  public DefaultOrbitTemplate(KubernetesClient k8sClient,
                              QosOptimizer qosOptimizer,
                              CrtConfig config) {
    this.k8sClient = k8sClient;
    this.config = config;
    this.qosOptimizer = qosOptimizer;
    tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public CrController create(OprcEnvironment env, DeploymentUnit deploymentUnit) {
    return new DeploymentCrController(
      this, k8sClient, env.config(), tsidFactory.create()
    );
  }

  public CrController load(OprcEnvironment env, ProtoCr cr) {
    return new DeploymentCrController(this, k8sClient, env.config(), cr);
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
