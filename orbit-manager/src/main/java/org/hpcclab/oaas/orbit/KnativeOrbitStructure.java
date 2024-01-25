package org.hpcclab.oaas.orbit;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hpcclab.oaas.orbit.env.OprcEnvironment;

public class KnativeOrbitStructure extends DeploymentOrbitStructure{
  public KnativeOrbitStructure(OrbitTemplate template,
                               KubernetesClient client,
                               OprcEnvironment.Config envConfig,
                               Tsid tsid) {
    super(template, client, envConfig, tsid);
  }
}
