package org.hpcclab.oaas.taskgen;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.oaas.iface.service.FunctionService;
import org.hpcclab.oaas.iface.service.ObjectService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.net.URI;

@ApplicationScoped
public class ClientProducer {
  @Inject
  TaskManagerConfig config;

  @Produces
  public ObjectService objectService() {
    return RestClientBuilder.newBuilder()
        .baseUri(URI.create(config.objectControllerUrl()))
        .build(ObjectService.class);
  }
  @Produces
  public FunctionService functionService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(config.objectControllerUrl()))
      .build(FunctionService.class);
  }

}
