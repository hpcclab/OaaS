package org.hpcclab.oaas.provisioner;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceStatus;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import static org.hpcclab.oaas.provisioner.KpConfig.LABEL_KEY;

@ApplicationScoped
public class FunctionWatcher {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionWatcher.class);
  @Inject
  KnativeClient knativeClient;
  @Inject
  FunctionRepository functionRepo;
  @Channel("fnUpdated")
  Emitter<OFunction> emitter;

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

  public void start(@Observes StartupEvent event) {
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
    var functionName = service.getMetadata().getLabels()
      .get(LABEL_KEY);
    if (functionName==null)
      return;
    OFunction fn = switch (action) {
      case MODIFIED -> {
        var condition = extractReadyCondition(service);
        if (condition.isEmpty())
          yield null;
        var ready = condition.get().getStatus().equals("True");
        var reason = condition.get().getReason();
        if (ready) {
          LOGGER.info("updating status {} to {}",
            functionName, DeploymentCondition.RUNNING);
          yield  functionRepo.compute(functionName, (k, f) -> {
            f.getDeploymentStatus()
              .setCondition(DeploymentCondition.RUNNING)
              .setInvocationUrl(service.getStatus().getAddress().getUrl())
              .setErrorMsg(null);
            return f;
          });
        } else if (reason!=null) {
          LOGGER.info("updating of status {} to {}",
            functionName, DeploymentCondition.DOWN);
          yield functionRepo.compute(functionName, (k, f) -> {
            f.getDeploymentStatus()
              .setCondition(DeploymentCondition.DOWN)
              .setErrorMsg(reason);
            return f;
          });
        }
        yield null;
      }
      case DELETED -> functionRepo.compute(functionName, (k, f) -> {
        f.getDeploymentStatus().setCondition(DeploymentCondition.DELETED)
          .setErrorMsg(null)
          .setInvocationUrl(null);
        return f;
      });
      case ERROR -> functionRepo.compute(functionName, (k, f) -> {
        f.getDeploymentStatus().setCondition(DeploymentCondition.DOWN);
        extractReadyCondition(service)
          .ifPresent(value -> f.getDeploymentStatus()
            .setErrorMsg(value.getReason())
          );
        return f;
      });
      default -> null;
    };
    pubUpdate(fn);
  }

  void pubUpdate(OFunction fn) {
    if (fn == null) return;
    var msg = Message.of(fn, Metadata.of(
        OutgoingKafkaRecordMetadata.builder()
          .withKey(fn.getKey())
          .withHeaders(new RecordHeaders()
            .add("oprc-provision-skip", "true".getBytes(StandardCharsets.UTF_8))
          )
          .build()
      )
    );
    emitter.send(msg);
  }
}
