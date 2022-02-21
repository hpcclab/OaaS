package org.hpcclab.oaas.controller;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.oaas.controller.service.DataAllocationService;
import org.hpcclab.oaas.iface.service.TaskExecutionService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.net.URI;

@ApplicationScoped
public class ClientProducer {
  @Inject
  OcConfig ocConfig;

  @Produces
  public TaskExecutionService taskExecutionService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(ocConfig.taskGeneratorUrl()))
      .build(TaskExecutionService.class);
  }

  @Produces
  public DataAllocationService dataAllocationService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(ocConfig.storageAdapterUrl()))
      .build(DataAllocationService.class);
  }

}
