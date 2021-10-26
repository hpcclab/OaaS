package org.hpcclab.msc.taskgen.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.task.OaasTask;
import org.hpcclab.oaas.entity.task.TaskFlow;
import org.hpcclab.oaas.service.ObjectService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFlowRepository implements ReactivePanacheMongoRepositoryBase<TaskFlow, String> {

  @Inject
  ObjectService objectService;

  public Uni<TaskFlow> find(OaasObject object, String requestFile) {
    var id = OaasTask.createId(object, requestFile);
    return findById(id);
  }
}
