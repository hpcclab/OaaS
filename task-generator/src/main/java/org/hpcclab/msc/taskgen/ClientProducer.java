package org.hpcclab.msc.taskgen;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.msc.object.service.FunctionService;
import org.hpcclab.msc.object.service.ObjectService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.net.URI;

@ApplicationScoped
public class ClientProducer {
  @ConfigProperty(name = "oaas.tg.objectControllerUrl")
  String objectControllerUrl;

  @Produces
  public ObjectService objectService() {
    return RestClientBuilder.newBuilder()
        .baseUri(URI.create(objectControllerUrl))
        .build(ObjectService.class);
  }
  @Produces
  public FunctionService functionService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(objectControllerUrl))
      .build(FunctionService.class);
  }

}
