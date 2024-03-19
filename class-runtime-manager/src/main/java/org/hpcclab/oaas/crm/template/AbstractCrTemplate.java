package org.hpcclab.oaas.crm.template;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;

import java.util.Map;
import java.util.Objects;

public abstract class AbstractCrTemplate implements ClassRuntimeTemplate {
  protected final TsidFactory tsidFactory;
  protected final KubernetesClient k8sClient;
  protected final CrtMappingConfig.CrtConfig config;
  protected final QosOptimizer qosOptimizer;

  protected AbstractCrTemplate(KubernetesClient k8sClient,
                               CrtMappingConfig.CrtConfig config,
                               QosOptimizer qosOptimizer) {

    Objects.requireNonNull(k8sClient);
    this.k8sClient = k8sClient;
    Objects.requireNonNull(config);
    this.config = validate(config);
    Objects.requireNonNull(qosOptimizer);
    this.qosOptimizer = qosOptimizer;
    this.tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public QosOptimizer getQosOptimizer() {
    return qosOptimizer;
  }

  protected static CrtMappingConfig.CrtConfig validate(CrtMappingConfig.CrtConfig crtConfig) {
    var func = crtConfig.functions();
    if (func==null) {
      func = CrtMappingConfig.FnConfig.builder()
        .stabilizationWindow(20000)
        .build();
    }
    String optimizer = crtConfig.optimizer();
    if (optimizer==null) optimizer = "default";
    Map<String, CrtMappingConfig.SvcConfig> services = crtConfig.services();
    for (var comp : OprcComponent.values()) {
      CrtMappingConfig.SvcConfig svcConfig = services.get(comp.getSvc());
      if (svcConfig==null) {
        svcConfig = CrtMappingConfig.SvcConfig.builder()
          .stabilizationWindow(30000)
          .maxScaleStep(2)
          .maxReplicas(20)
          .build();
        services.put(comp.getSvc(), svcConfig);
      }
    }

    return crtConfig.toBuilder()
      .functions(func)
      .optimizer(optimizer)
      .build();
  }
}
