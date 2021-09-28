package org.hpcclab.msc.object;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.hpcclab.msc.object.service.ObjectService;
import org.hpcclab.msc.object.service.ResourceRequestService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.net.URI;

@ApplicationScoped
public class ClientProducer {
  @Inject
  OcConfig ocConfig;

  @Produces
  public ResourceRequestService resourceRequestService() {
    return RestClientBuilder.newBuilder()
      .baseUri(URI.create(ocConfig.taskGeneratorUrl()))
      .build(ResourceRequestService.class);
  }

}
