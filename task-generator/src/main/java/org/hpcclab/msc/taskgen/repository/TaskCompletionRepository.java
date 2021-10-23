package org.hpcclab.msc.taskgen.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepositoryBase;
import org.hpcclab.oaas.entity.task.TaskCompletion;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskCompletionRepository implements ReactivePanacheMongoRepositoryBase<TaskCompletion, String> {

}
