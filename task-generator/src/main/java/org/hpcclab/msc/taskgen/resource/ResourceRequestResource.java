package org.hpcclab.msc.taskgen.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.ObjectResourceRequest;
import org.hpcclab.oaas.service.ResourceRequestService;
import org.hpcclab.msc.taskgen.TaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ResourceRequestResource implements ResourceRequestService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceRequestResource.class);

  @Inject
  TaskHandler taskHandler;

//  @Override
//  public Uni<TaskFlow> request(ObjectResourceRequest request) {
//    return taskHandler.handle(request);
//  }
//

  @Override
  public Uni<Void> request(ObjectResourceRequest request) {
    return null;
  }
}
