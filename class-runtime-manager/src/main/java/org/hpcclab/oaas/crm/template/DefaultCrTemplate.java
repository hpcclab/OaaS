package org.hpcclab.oaas.crm.template;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.CrtMappingConfig.CrtConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.controller.*;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.crm.observe.FnEventObserver;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentUnit;
import org.hpcclab.oaas.proto.ProtoCr;

public class DefaultCrTemplate extends AbstractCrTemplate {


  public DefaultCrTemplate(String name,
                           KubernetesClient k8sClient,
                           QosOptimizer qosOptimizer,
                           CrtConfig config,
                           CrmConfig crmConfig) {
    super(name, k8sClient, config, qosOptimizer, crmConfig);
  }

  @Override
  public void init(CrControllerManager crControllerManager, EnvironmentManager environmentManager) {
    FnEventObserver fnEventObserver = FnEventObserver.getOrCreate(
      type(),
      new DefaultKnativeClient(k8sClient),
      crControllerManager,
      environmentManager
    );
    fnEventObserver.start(K8SCrController.CR_FN_KEY);
  }

  @Override
  public CrtConfig getConfig() {
    return config;
  }

  @Override
  public CrController create(OprcEnvironment.Config envConf, DeploymentUnit deploymentUnit) {
    var invoker = new InvokerK8sCrComponentController(config.services().get(OprcComponent.INVOKER.getSvc()), crmConfig);
    var sa = new SaK8sCrComponentController(config.services().get(OprcComponent.STORAGE_ADAPTER.getSvc()));
    var conf = new ConfigK8sCrComponentController(null);
    var kn = new KnativeCrFnController(config.functions(), envConf);
    var dep = new DeploymentCrFnController(config.functions());
    return new K8SCrController(
      this,
      k8sClient,
      invoker,
      sa,
      conf,
      dep,
      kn,
      envConf,
      tsidFactory.create()
    );
  }

  @Override
  public CrController load(OprcEnvironment.Config env, ProtoCr cr) {
    var invoker = new InvokerK8sCrComponentController(config.services().get(OprcComponent.INVOKER.getSvc()), crmConfig);
    var sa = new SaK8sCrComponentController(config.services().get(OprcComponent.STORAGE_ADAPTER.getSvc()));
    var conf = new ConfigK8sCrComponentController(null);
    var kn = new KnativeCrFnController(config.functions(), env);
    var dep = new DeploymentCrFnController(config.functions());
    return new K8SCrController(this, k8sClient,
      invoker,
      sa,
      conf,
      dep,
      kn,
      env,
      cr);
  }

  @Override
  public String type() {
    return "default";
  }
}
