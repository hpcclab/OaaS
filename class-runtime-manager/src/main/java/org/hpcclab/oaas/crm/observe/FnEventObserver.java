package org.hpcclab.oaas.crm.observe;

import com.github.f4b6a3.tsid.Tsid;
import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.controller.K8SCrController;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoOFunction;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static org.hpcclab.oaas.proto.ProtoDeploymentCondition.*;


public class FnEventObserver {
  private static final Logger logger = LoggerFactory.getLogger(FnEventObserver.class);

  final KnativeClient knativeClient;
  final DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;
  Watch watch;

  public FnEventObserver(KnativeClient knativeClient,
                         DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater) {
    this.knativeClient = knativeClient;
    Objects.requireNonNull(deploymentStatusUpdater);
    this.deploymentStatusUpdater = deploymentStatusUpdater;
  }

  public void start(String label) {
    logger.info("start kn function watcher");
    watch = knativeClient.services()
      .withLabel(label)
      .watch(new FnEventWatcher(deploymentStatusUpdater));
  }

  public void stop() {
    watch.close();
  }


  public static class FnEventWatcher implements Watcher<Service> {

    DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater;
    CrControllerManager controllerManager;

    public FnEventWatcher(
      DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub deploymentStatusUpdater) {
      Objects.requireNonNull(deploymentStatusUpdater);
      this.deploymentStatusUpdater = deploymentStatusUpdater;
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

    public void updateToUp(Service svc) {
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToUp [{}, {}]", crId, fnKey);
      if (crId==null)
        return;
      if (fnKey==null)
        return;

      var status = ProtoOFunctionDeploymentStatus.newBuilder()
        .setCondition(PROTO_DEPLOYMENT_CONDITION_RUNNING)
        .setErrorMsg("")
        .setInvocationUrl(svc.getStatus().getAddress().getUrl())
        .build();
      update(crId, fnKey, status);
    }


    public void updateToDown(Service svc, String msg) {
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToDown [{}, {}]", crId, fnKey);
      if (crId==null)
        return;
      if (fnKey==null)
        return;
      ProtoOFunctionDeploymentStatus status = ProtoOFunctionDeploymentStatus.newBuilder()
        .setCondition(PROTO_DEPLOYMENT_CONDITION_DOWN)
        .setErrorMsg(msg==null ? "":msg)
        .build();
      update(crId, fnKey, status);
    }

    public void updateToDelete(Service svc) {
      var crId = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_LABEL_KEY);
      var fnKey = svc.getMetadata().getLabels()
        .get(K8SCrController.CR_FN_KEY);
      logger.info("updateToDelete [{}, {}]", crId, fnKey);
      if (crId==null)
        return;
      if (fnKey==null)
        return;

      ProtoOFunctionDeploymentStatus status = ProtoOFunctionDeploymentStatus.newBuilder()
        .setCondition(PROTO_DEPLOYMENT_CONDITION_DELETED)
        .setErrorMsg("")
        .build();

      update(crId, fnKey, status);
    }

    private void update(String crId, String fnKey, ProtoOFunctionDeploymentStatus status) {
      var id = Tsid.from(crId).toLong();
      var controller = controllerManager.get(id);
      ProtoOFunction func = controller.getAttachedFn().get(fnKey);
      ProtoOFunction newFunc = func.toBuilder()
        .setStatus(status)
        .build();
      controller.getAttachedFn().put(fnKey, func);
      deploymentStatusUpdater.updateFn(OFunctionStatusUpdate.newBuilder()
        .setKey(fnKey)
        .setStatus(status)
        .setProvision(newFunc.getProvision())
        .build());
    }
    @Override
    public void onClose(WatcherException cause) {
      logger.error("watcher is closed", cause);
    }
  }
}
