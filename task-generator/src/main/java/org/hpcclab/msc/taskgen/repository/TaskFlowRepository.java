package org.hpcclab.msc.taskgen.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.task.Task;
import org.hpcclab.msc.object.service.ObjectService;
import org.hpcclab.msc.object.entity.task.TaskFlow;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class TaskFlowRepository implements ReactivePanacheMongoRepositoryBase<TaskFlow, String> {

  @Inject
  ObjectService objectService;

  public Uni<TaskFlow> find(MscObject object, String requestFile) {
    var id = Task.createId(object, requestFile);
    return findById(id);
  }
}
