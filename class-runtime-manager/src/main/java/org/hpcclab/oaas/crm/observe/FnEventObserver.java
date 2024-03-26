package org.hpcclab.oaas.crm.observe;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.knative.internal.pkg.apis.duck.v1.Addressable;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.controller.K8SCrController;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.env.OprcEnvironment;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.hpcclab.oaas.proto.ProtoDeploymentCondition.*;


public class FnEventObserver {
  private static final Logger logger = LoggerFactory.getLogger(FnEventObserver.class);

  static Map<String, FnEventObserver> observers = new ConcurrentHashMap<>();
  final KnativeClient knativeClient;
  final CrControllerManager controllerManager;
  final EnvironmentManager environmentManager;

  Watch watch;

  public FnEventObserver(KnativeClient knativeClient,
                         CrControllerManager controllerManager,
                         EnvironmentManager environmentManager) {
    this.knativeClient = knativeClient;
    this.controllerManager = controllerManager;
    this.environmentManager = environmentManager;
  }

  public static FnEventObserver getOrCreate(String name,
                                            KnativeClient knativeClient,
                                            CrControllerManager controllerManager,
                                            EnvironmentManager environmentManager
  ) {
    return observers.computeIfAbsent(name, n -> new FnEventObserver(knativeClient, controllerManager, environmentManager));
  }

  public void start(String label) {
    if (watch==null) {
      logger.info("start kn functions watcher");
      watch = knativeClient.services()
        .withLabel(label)
        .watch(new FnEventWatcher(controllerManager, environmentManager));
    }
  }

  public void stop() {
    if (watch!=null)
      watch.close();
  }


  public static class FnEventWatcher implements Watcher<Service> {
    final CrControllerManager controllerManager;
    final EnvironmentManager environmentManager;

    public FnEventWatcher(
      CrControllerManager controllerManager, EnvironmentManager environmentManager) {
      this.controllerManager = controllerManager;
      this.environmentManager = environmentManager;
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
    public boolean reconnecting() {
      return true;
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

      OprcEnvironment.Config environmentConfig = environmentManager.getEnvironmentConfig();
      var url = environmentConfig.useKnativeLb() ?
        svc.getStatus().getUrl():
        svc.getStatus().getAddress().getUrl();
      var status = ProtoOFunctionDeploymentStatus.newBuilder()
        .setCondition(PROTO_DEPLOYMENT_CONDITION_RUNNING)
        .setErrorMsg("")
        .setInvocationUrl(url)
        .build();
      controllerManager.update(crId, fnKey, status);
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


      OprcEnvironment.Config environmentConfig = environmentManager.getEnvironmentConfig();
      Optional<ServiceStatus> svcStatus = Optional.of(svc.getStatus());
      Optional<String> urlOptional = environmentConfig.useKnativeLb() ?
        svcStatus.map(ServiceStatus::getUrl):
        svcStatus.map(ServiceStatus::getAddress).map(Addressable::getUrl);
      ProtoOFunctionDeploymentStatus status = ProtoOFunctionDeploymentStatus.newBuilder()
        .setCondition(PROTO_DEPLOYMENT_CONDITION_DOWN)
        .setErrorMsg(msg==null ? "":msg)
        .setInvocationUrl(urlOptional.orElse(""))
        .build();
      controllerManager.update(crId, fnKey, status);
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

      controllerManager.update(crId, fnKey, status);
    }


    @Override
    public void onClose(WatcherException cause) {
      logger.error("watcher is closed", cause);
    }
  }
}
