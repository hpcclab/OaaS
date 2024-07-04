package org.hpcclab.oaas.crm.template;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CpuBasedQoSOptimizer;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.model.exception.StdOaasException;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class CrTemplateFactoryImpl implements CrTemplateFactory {
  public static final String DEFAULT = "default";

  final KubernetesClient kubernetesClient;
  final CrmConfig crmConfig;

  public CrTemplateFactoryImpl(KubernetesClient kubernetesClient, CrmConfig crmConfig) {
    this.kubernetesClient = kubernetesClient;
    this.crmConfig = crmConfig;
  }


  @Override
  public CrTemplate create(String name, CrtMappingConfig.CrtConfig config) {
    if (config.type().equals(DEFAULT)) {
      return new DefaultCrTemplate(
        name,
        kubernetesClient,
        this::selectOptimizer,
        config,
        crmConfig
      );
    } else {
      throw new StdOaasException("No available CR template with type " + config.type());
    }
  }

  public QosOptimizer selectOptimizer(CrtMappingConfig.CrtConfig config) {
    return new CpuBasedQoSOptimizer(config);
  }
}
