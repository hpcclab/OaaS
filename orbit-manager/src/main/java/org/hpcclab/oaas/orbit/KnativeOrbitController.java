package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.orbit.controller.DeploymentOrbitController;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;

public class KnativeOrbitController extends DeploymentOrbitController {
  public KnativeOrbitController(OrbitTemplate template,
                                KubernetesClient client,
                                OprcEnvironment.Config envConfig,
                                Tsid tsid) {
    super(template, client, envConfig, tsid);
  }
}
