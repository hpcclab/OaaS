package org.hpcclab.oaas.taskmanager;

import org.hpcclab.oaas.repository.function.InvocationGraphExecutor;
import org.hpcclab.oaas.repository.function.RepoContextLoader;
import org.hpcclab.oaas.repository.function.TaskSubmitter;
import org.hpcclab.oaas.repository.impl.RepoGraphStateManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class InvocationEngineProducer {
  @Inject
  TaskManagerConfig config;

  @Inject
  TaskSubmitter taskSubmitter;
  @Inject
  RepoGraphStateManager graphStateManager;
  @Inject
  RepoContextLoader contextLoader;

  @Produces
  InvocationGraphExecutor invocationGraphExecutor() {
    return new InvocationGraphExecutor(taskSubmitter,graphStateManager, contextLoader);
  }

}
