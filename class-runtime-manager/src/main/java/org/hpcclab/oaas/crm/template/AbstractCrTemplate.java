package org.hpcclab.oaas.crm.template;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrComponent;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractCrTemplate implements CrTemplate {
  protected final TsidFactory tsidFactory;
  protected final KubernetesClient k8sClient;
  protected final CrtMappingConfig.CrtConfig config;
  protected final QosOptimizer qosOptimizer;
  protected final String name;
  protected final CrmConfig crmConfig;

  protected AbstractCrTemplate(String name,
                               KubernetesClient k8sClient,
                               CrtMappingConfig.CrtConfig config,
                               Function<CrtMappingConfig.CrtConfig, QosOptimizer> optimizerBuilder,
                               CrmConfig crmConfig) {

    this.name = name;
    this.crmConfig = crmConfig;
    Objects.requireNonNull(k8sClient);
    this.k8sClient = k8sClient;
    Objects.requireNonNull(config);
    this.config = validate(config);
    Objects.requireNonNull(optimizerBuilder);
    this.qosOptimizer = optimizerBuilder.apply(this.config);
    this.tsidFactory = TsidFactory.newInstance1024();

  }

  @Override
  public QosOptimizer getQosOptimizer() {
    return qosOptimizer;
  }

  protected CrtMappingConfig.CrtConfig validate(CrtMappingConfig.CrtConfig crtConfig) {
    var func = crtConfig.functions();
    if (func==null) {
      func = CrtMappingConfig.FnConfig.builder()
        .stabilizationWindow(20000)
        .defaultMaxScale(10)
        .build();
    }
    if (func.defaultMaxScale() <= 0) {
      func = func.toBuilder().defaultMaxScale(10).build();
    }
    if (func.maxScaleStep() <= 0) {
      func = func.toBuilder().maxScaleStep(3).build();
    }
    String optimizer = crtConfig.optimizer();
    if (optimizer==null) optimizer = "default";
    Map<String, CrtMappingConfig.CrComponentConfig> services = crtConfig.services();
    for (var comp : CrComponent.values()) {
      CrtMappingConfig.CrComponentConfig svcConfig = services.get(comp.getSvc());
      if (svcConfig==null) {
        svcConfig = CrtMappingConfig.CrComponentConfig.builder()
          .stabilizationWindow(30000)
          .maxScaleStep(2)
          .maxReplicas(10)
          .build();
        services.put(comp.getSvc(), svcConfig);
      }
    }

    return crtConfig.toBuilder()
      .functions(func)
      .optimizer(optimizer)
      .build();
  }

  @Override
  public String name() {
    return name;
  }
}
