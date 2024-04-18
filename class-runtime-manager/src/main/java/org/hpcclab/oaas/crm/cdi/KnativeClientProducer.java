package org.hpcclab.oaas.crm.cdi;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.kubernetes.client.Config;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Singleton
public class KnativeClientProducer {

  @Produces
  public KnativeClient knativeClient(Config config) {
    return new DefaultKnativeClient(config);
  }
}
