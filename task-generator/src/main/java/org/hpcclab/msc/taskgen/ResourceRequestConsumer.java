package org.hpcclab.msc.taskgen;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.object.model.Task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ResourceRequestConsumer {

  @Inject
  TaskHandler taskHandler;

  @Incoming("resource-requests")
  public Uni<Task> handle(ObjectResourceRequest request) {
    return taskHandler.handle(request);
  }

}
