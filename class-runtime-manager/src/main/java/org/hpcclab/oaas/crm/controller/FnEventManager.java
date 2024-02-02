package org.hpcclab.oaas.crm.controller;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.proto.DeploymentStatusUpdater;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

import static org.hpcclab.oaas.proto.ProtoDeploymentCondition.*;
import static org.hpcclab.oaas.proto.ProtoDeploymentCondition.PROTO_DEPLOYMENT_CONDITION_DOWN;


@ApplicationScoped
public class FnEventManager {
  private static final Logger logger = LoggerFactory.getLogger( FnEventManager.class );

  KnativeClient knativeClient;
  DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;
  Watch watch;
  @Inject
  public FnEventManager(KnativeClient knativeClient,
                        @GrpcClient("package-manager")
                        DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater) {
    this.knativeClient = knativeClient;
    this.deploymentStatusUpdater = deploymentStatusUpdater;
  }

  public void start() {
    logger.info("start kn function watcher");
    watch = knativeClient.services()
      .withLabel(K8SCrController.CR_FN_KEY)
      .watch(new FnEventWatcher(deploymentStatusUpdater));
  }


  public static class FnEventWatcher implements Watcher<Service> {

    DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;

    public FnEventWatcher(
      DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater) {
      this.deploymentStatusUpdater = deploymentStatusUpdater;
    }

    @Override
    public void eventReceived(Action action, Service service) {
      switch (action) {
        case MODIFIED -> {
          var condition = extractReadyCondition(service);
          if (condition.isEmpty())
            break;
          var ready = condition.get().getStatus().equals("True");
          var reason = condition.get().getReason();
          if (ready) {
            updateToUp(service);
          } else if (reason!=null) {
            updateToDown(service, reason);
          }
        }
        case DELETED -> updateToDelete(service);
        case ERROR -> {
          var msg = extractReadyCondition(service)
            .map(Condition::getReason)
            .orElse("");
          updateToDown(service, msg);
        }
      }
    }

    public void updateToUp(Service svc){
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToUp [{}, {}]", crId, fnKey);
      if (crId == null)
        return;
      if (fnKey == null)
        return;
      deploymentStatusUpdater.updateFn(OFunctionStatusUpdate.newBuilder()
          .setKey(fnKey)
          .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
            .setCondition(PROTO_DEPLOYMENT_CONDITION_RUNNING)
            .setErrorMsg("")
            .setInvocationUrl(svc.getStatus().getAddress().getUrl())
            .build())
        .build());
    }
    public void updateToDown(Service svc, String msg){
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToDown [{}, {}]", crId, fnKey);
      if (crId == null)
        return;
      if (fnKey == null)
        return;
      deploymentStatusUpdater.updateFn(OFunctionStatusUpdate.newBuilder()
        .setKey(fnKey)
        .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
          .setCondition(PROTO_DEPLOYMENT_CONDITION_DOWN)
          .setErrorMsg(msg == null? "" : msg)
          .build())
        .build());
    }
    public void updateToDelete(Service svc){
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToDelete [{}, {}]", crId, fnKey);
      if (crId == null)
        return;
      if (fnKey == null)
        return;
      deploymentStatusUpdater.updateFn(OFunctionStatusUpdate.newBuilder()
        .setKey(fnKey)
        .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
          .setCondition(PROTO_DEPLOYMENT_CONDITION_DELETED)
          .setErrorMsg("")
          .build())
        .build());
    }

    public static Optional<Condition> extractReadyCondition(Service service) {
      return Optional.of(service)
        .map(Service::getStatus)
        .map(ServiceStatus::getConditions)
        .stream()
        .flatMap(Collection::stream)
        .filter(c -> c.getType().equals("Ready"))
        .findAny();
    }

    @Override
    public void onClose(WatcherException cause) {
      logger.error("watcher is closed", cause);
    }
  }
}
