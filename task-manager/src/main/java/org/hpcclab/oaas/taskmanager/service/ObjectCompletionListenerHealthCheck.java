package org.hpcclab.oaas.taskmanager.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Liveness
public class ObjectCompletionListenerHealthCheck implements HealthCheck {
  @Inject
  ObjectCompletionListener listener;
  @Inject
  TaskManagerConfig config;
  private static final String NAME = "Object Completion Listener";

  @Override
  public HealthCheckResponse call() {
    if (config.enableCompletionListener() || listener.watcher != null){
      return HealthCheckResponse.up(NAME);
    }
    return HealthCheckResponse.down(NAME);
  }
}
