package org.hpcclab.oaas.crm;

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
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.proto.DeploymentStatusUpdaterGrpc;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;
import org.hpcclab.oaas.proto.ProtoOFunctionDeploymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

import static org.hpcclab.oaas.crm.CrmConfig.LABEL_KEY;
import static org.hpcclab.oaas.proto.ProtoDeploymentCondition.*;

@ApplicationScoped
public class FunctionWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionWatcher.class);
  @Inject
  KnativeClient knativeClient;
  @GrpcClient("class-manager")
  DeploymentStatusUpdaterGrpc.DeploymentStatusUpdaterBlockingStub updater;
  @Inject
  ProtoMapper mapper;

  Watch watch;

  public static Optional<Condition> extractReadyCondition(Service service) {
    return Optional.of(service)
      .map(Service::getStatus)
      .map(ServiceStatus::getConditions)
      .stream()
      .flatMap(Collection::stream)
      .filter(c -> c.getType().equals("Ready"))
      .findAny();
  }

  public void start(
//    @Observes StartupEvent event
  ) {
    watch = knativeClient.services()
      .withLabel(LABEL_KEY)
      .watch(new Watcher<Service>() {
        @Override
        public void eventReceived(Action action, Service service) {
          handleEvent(action, service);
        }

        @Override
        public void onClose(WatcherException cause) {
          LOGGER.error("watcher is closed", cause);
        }
      });
  }

  private void handleEvent(Watcher.Action action, Service service) {
    var fnKey = service.getMetadata().getLabels()
      .get(LABEL_KEY);
    if (fnKey==null)
      return;
    switch (action) {
      case MODIFIED -> {
        var condition = extractReadyCondition(service);
        if (condition.isEmpty())
          break;
        var ready = condition.get().getStatus().equals("True");
        var reason = condition.get().getReason();
        if (ready) {
          LOGGER.info("updating status {} to {}",
            fnKey, DeploymentCondition.RUNNING);
          updater.updateFn(OFunctionStatusUpdate.newBuilder()
            .setKey(fnKey)
            .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
              .setCondition(PROTO_DEPLOYMENT_CONDITION_RUNNING)
              .setInvocationUrl(service.getStatus().getAddress().getUrl())
            )
            .build());

        } else if (reason!=null) {
          LOGGER.info("updating of status {} to {}",
            fnKey, DeploymentCondition.DOWN);
          updater.updateFn(OFunctionStatusUpdate.newBuilder()
            .setKey(fnKey)
            .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
              .setCondition(PROTO_DEPLOYMENT_CONDITION_DOWN)
              .setErrorMsg(reason)
            )
            .build());
        }
      }
      case DELETED -> updater.updateFn(OFunctionStatusUpdate.newBuilder()
        .setKey(fnKey)
        .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
          .setCondition(PROTO_DEPLOYMENT_CONDITION_DELETED)
        )
        .build());

      case ERROR -> {
        var msg = extractReadyCondition(service)
          .map(Condition::getReason)
          .orElse("");
        updater.updateFn(OFunctionStatusUpdate.newBuilder()
          .setKey(fnKey)
          .setStatus(ProtoOFunctionDeploymentStatus.newBuilder()
            .setCondition(PROTO_DEPLOYMENT_CONDITION_DOWN)
            .setErrorMsg(msg)
          )
          .build());
      }
    }
  }
//    pubUpdate(mapper.fromProto(fn));

//  void pubUpdate(OFunction fn) {
//    if (fn==null) return;
//    var msg = Message.of(fn, Metadata.of(
//        OutgoingKafkaRecordMetadata.builder()
//          .withKey(fn.getKey())
//          .withHeaders(new RecordHeaders()
//            .add("oprc-provision-skip", "true".getBytes(StandardCharsets.UTF_8))
//          )
//          .build()
//      )
//    );
//    emitter.send(msg);
}
