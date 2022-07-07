package org.hpcclab.oaas.taskmanager.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.hpcclab.oaas.repository.impl.OaasObjectRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Liveness
public class ObjectCompletionListenerHealthCheck implements HealthCheck {
  @Inject
  ObjectCompletionListener listener;
  @Inject
  OaasObjectRepository objectRepo;

  @Override
  public HealthCheckResponse call() {
    var listeners = objectRepo.getRemoteCache().getListeners();
    if (listeners.contains(listener.watcher)) {
      return HealthCheckResponse.up("Object Completion Listener");
    }
    return HealthCheckResponse.down("Object Completion Listener");
  }
}
