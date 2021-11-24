package org.hpcclab.oaas.provisioner;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class KnativeClientProducer {

  @Produces
  public KnativeClient knativeClient(Config config) {
    return new DefaultKnativeClient(config);
  }
}
