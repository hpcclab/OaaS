package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public abstract class AbstractCrFnController implements CrFnController<HasMetadata> {
  protected final CrtMappingConfig.FnConfig fnConfig;
  final Map<String, Long> stabilizationTimeMap = Maps.mutable.empty();
  protected CrController parent;
  protected KubernetesClient kubernetesClient;
  protected String prefix;
  protected String namespace;
  protected Map<String, CrInstanceSpec> currentSpecs = Maps.mutable.empty();

  protected AbstractCrFnController(CrtMappingConfig.FnConfig fnConfig) {
    this.fnConfig = fnConfig;
  }

  @Override
  public void init(CrController parentController) {
    parent = parentController;
    if (parentController instanceof K8SCrController k8SCrController) {
      this.kubernetesClient = k8SCrController.kubernetesClient;
      this.prefix = k8SCrController.prefix;
      this.namespace = k8SCrController.namespace;
    } else
      throw new IllegalArgumentException("Parent cr controller is not a K8SCrController");
  }

  @Override
  public void updateStabilizationTime(String key) {
    stabilizationTimeMap.put(key,
      System.currentTimeMillis() + fnConfig.stabilizationWindow()
    );
  }

  @Override
  public FnResourcePlan applyAdjustment(CrAdjustmentPlan plan) {
    long currentTimeMillis = System.currentTimeMillis();
    List<HasMetadata> resources = Lists.mutable.empty();
    for (var entry : plan.fnInstances().entrySet()) {
      if (stabilizationTimeMap.getOrDefault(entry.getKey(), 0L) > currentTimeMillis) {
        continue;
      }
      resources.addAll(doApplyAdjustment(entry.getKey(), entry.getValue()));
      currentSpecs.put(entry.getKey(), entry.getValue());
    }
    return new FnResourcePlan(
      resources,
      List.of()
    );
  }

  protected abstract List<HasMetadata> doApplyAdjustment(String fnKey, CrInstanceSpec spec);

  @Override
  public Map<String, CrInstanceSpec> currentSpecs() {
    return currentSpecs;
  }
}
