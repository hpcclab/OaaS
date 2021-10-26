package org.hpcclab.oaas.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import org.hpcclab.oaas.entity.task.TaskDependent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TaskDependentRepository implements PanacheRepositoryBase<TaskDependent, String> {

}
