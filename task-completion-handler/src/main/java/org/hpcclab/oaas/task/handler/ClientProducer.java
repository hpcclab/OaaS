package org.hpcclab.oaas.task.handler;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.oaas.iface.service.ObjectService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.net.URI;

@ApplicationScoped
public class ClientProducer {
  @ConfigProperty(name = "oaas.tch.objectControllerUrl")
  String objectControllerUrl;

  @Produces
  public ObjectService taskExecutionService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(objectControllerUrl))
      .build(ObjectService.class);
  }

}
