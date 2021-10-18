package org.hpcclab.msc.taskgen.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.service.ResourceRequestService;
import org.hpcclab.msc.taskgen.TaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
