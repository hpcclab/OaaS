package org.hpcclab.oaas.taskmanager.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;
import org.hpcclab.oaas.taskmanager.TaskManagerConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
    if (listener.healthcheck()){
      return HealthCheckResponse.up(NAME);
    }
    return HealthCheckResponse.down(NAME);
  }
}
