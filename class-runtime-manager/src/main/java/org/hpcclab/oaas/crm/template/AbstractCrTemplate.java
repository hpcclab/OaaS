package org.hpcclab.oaas.crm.template;

import com.github.f4b6a3.tsid.TsidFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.QosOptimizer;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;

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
    this.config = config;
    Objects.requireNonNull(qosOptimizer);
    this.qosOptimizer = qosOptimizer;
    this.tsidFactory = TsidFactory.newInstance1024();
  }

  @Override
  public QosOptimizer getQosOptimizer() {
    return qosOptimizer;
  }
}
