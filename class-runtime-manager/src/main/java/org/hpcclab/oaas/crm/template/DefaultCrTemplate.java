package org.hpcclab.oaas.crm.template;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.CrtMappingConfig.CrtConfig;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.controller.K8SCrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.observe.FnEventObserver;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public class DefaultCrTemplate extends AbstractCrTemplate {


  public DefaultCrTemplate(KubernetesClient k8sClient,
                           QosOptimizer qosOptimizer,
                           CrtConfig config,
                           DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub statusUpdater) {
    super(k8sClient, config, qosOptimizer, statusUpdater);
  }

  @Override
  public void init(CrControllerManager crControllerManager) {
    FnEventObserver fnEventObserver = new FnEventObserver(new DefaultKnativeClient(k8sClient),
      statusUpdater, crControllerManager);
    fnEventObserver.start(K8SCrController.CR_FN_KEY);
  }

  @Override
  public CrtConfig getConfig() {
    return config;
  }

  @Override
  public CrController create(OprcEnvironment env, DeploymentUnit deploymentUnit) {
    return new K8SCrController(
      this, k8sClient, env.config(), tsidFactory.create()
    );
  }

  @Override
  public CrController load(OprcEnvironment env, ProtoCr cr) {
    return new K8SCrController(this, k8sClient, env.config(), cr);
  }

  @Override
  public String type() {
    return "default";
  }
}
