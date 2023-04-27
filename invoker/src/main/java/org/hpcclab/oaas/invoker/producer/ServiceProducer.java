package org.hpcclab.oaas.invoker.producer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.invoker.service.DataAllocationService;

import java.net.URI;

@ApplicationScoped
public class ServiceProducer {
  @Inject
  InvokerConfig invokerConfig;

  @Produces
  DataAllocationService dataAllocationService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(invokerConfig.storageAdapterUrl()))
      .build(DataAllocationService.class);
  }
}
