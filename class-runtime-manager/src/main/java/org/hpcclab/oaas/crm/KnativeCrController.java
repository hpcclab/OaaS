package org.hpcclab.oaas.crm;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.crm.controller.DeploymentCrController;
import org.hpcclab.oaas.crm.env.OprcEnvironment;

public class KnativeCrController extends DeploymentCrController {
  public KnativeCrController(OrbitTemplate template,
                             KubernetesClient client,
                             OprcEnvironment.Config envConfig,
                             Tsid tsid) {
    super(template, client, envConfig, tsid);
  }
}
