package org.hpcclab.oaas.crm.template;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.CrtMappingConfig.CrtConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.*;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.observe.FnEventObserver;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public class DefaultCrTemplate extends AbstractCrTemplate {


  public DefaultCrTemplate(String name,
                           KubernetesClient k8sClient,
                           QosOptimizer qosOptimizer,
                           CrtConfig config) {
    super(name, k8sClient, config, qosOptimizer);
  }

  @Override
  public void init(CrControllerManager crControllerManager) {
    FnEventObserver fnEventObserver = new FnEventObserver(
      new DefaultKnativeClient(k8sClient),
      crControllerManager);
    fnEventObserver.start(K8SCrController.CR_FN_KEY);
  }

  @Override
  public CrtConfig getConfig() {
    return config;
  }

  @Override
  public CrController create(OprcEnvironment env, DeploymentUnit deploymentUnit) {
    var invoker = new InvokerK8sCrComponentController(config.services().get(OprcComponent.INVOKER.getSvc()));
    var sa = new SaK8sCrComponentController(config.services().get(OprcComponent.STORAGE_ADAPTER.getSvc()));
    var conf = new ConfigK8sCrComponentController(null);
    var kn = new KnativeCrFnController(config.functions(), env.config());
    var dep = new DeploymentCrFnController(config.functions());
    return new K8SCrController(
      this,
      k8sClient,
      invoker,
      sa,
      conf,
      dep,
      kn,
      env.config(),
      tsidFactory.create()
    );
  }

  @Override
  public CrController load(OprcEnvironment env, ProtoCr cr) {
    var invoker = new InvokerK8sCrComponentController(config.services().get(OprcComponent.INVOKER.getSvc()));
    var sa = new SaK8sCrComponentController(config.services().get(OprcComponent.STORAGE_ADAPTER.getSvc()));
    var conf = new ConfigK8sCrComponentController(null);
    var kn = new KnativeCrFnController(config.functions(), env.config());
    var dep = new DeploymentCrFnController(config.functions());
    return new K8SCrController(this, k8sClient,
      invoker,
      sa,
      conf,
      dep,
      kn,
      env.config(),
      cr);
  }

  @Override
  public String type() {
    return "default";
  }
}
