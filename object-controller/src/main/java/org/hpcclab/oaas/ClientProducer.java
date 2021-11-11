package org.hpcclab.oaas;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
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

}
