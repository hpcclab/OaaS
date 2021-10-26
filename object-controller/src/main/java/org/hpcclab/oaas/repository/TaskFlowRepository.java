package org.hpcclab.oaas.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import org.hpcclab.oaas.entity.task.TaskFlow;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskFlowRepository implements PanacheRepositoryBase<TaskFlow, String> {

}
