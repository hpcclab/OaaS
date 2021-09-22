package org.hpcclab.msc.taskgen.resource;

import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource;
import io.quarkus.rest.data.panache.MethodProperties;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.task.TaskFlow;
import org.hpcclab.msc.object.model.ObjectResourceRequest;
import org.hpcclab.msc.taskgen.TaskHandler;
import org.hpcclab.msc.taskgen.repository.TaskFlowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

public interface TaskFlowResource extends PanacheMongoRepositoryResource<TaskFlowResource.InternalTaskFlowRepository, TaskFlow, String> {

  @MethodProperties(exposed = false)
  TaskFlow add(TaskFlow entity);

  @MethodProperties(exposed = false)
  TaskFlow update(String id, TaskFlow entity);

  @MethodProperties(exposed = false)
  boolean delete(String id);

  class InternalTaskFlowRepository implements PanacheMongoRepositoryBase<TaskFlow, String>{

  }
}
