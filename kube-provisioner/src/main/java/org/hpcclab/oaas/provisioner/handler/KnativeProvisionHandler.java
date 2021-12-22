package org.hpcclab.oaas.provisioner.handler;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerBuilder;
import io.fabric8.knative.eventing.v1.TriggerFilter;
import io.fabric8.knative.eventing.v1.TriggerSpec;
import io.fabric8.knative.flows.v1.*;
import io.fabric8.knative.internal.pkg.apis.duck.v1.Destination;
import io.fabric8.knative.internal.pkg.apis.duck.v1.KReference;
import io.fabric8.knative.messaging.v1.ChannelTemplateSpec;
import io.fabric8.knative.serving.v1.*;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RegisterForReflection(targets = {
  Service.class,
  ServiceSpec.class,
  TrafficTarget.class,
  RevisionTemplateSpec.class,
  RevisionSpec.class,
  Sequence.class,
  SequenceSpec.class,
  SequenceStep.class,
  ChannelTemplateSpec.class,
  KReference.class,
  Trigger.class,
  TriggerSpec.class,
  TriggerFilter.class,
  Destination.class
})
public class KnativeProvisionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(KnativeProvisionHandler.class);
  final String labelKey = "oaas.function";
  @Inject
  KnativeClient knativeClient;
  @ConfigProperty(name = "oaas.kp.taskHandlerService")
  String taskHandler;
  @ConfigProperty(name = "oaas.kp.exposeKnative")
  boolean exposeKnative;

  @Incoming("provisions")
  public void provision(Record<String,OaasFunctionDto> functionRecord) {
    var key = functionRecord.key();
    LOGGER.debug("Received Knative provision: {}", key);
    var function = functionRecord.value();
    if (function == null) {
      boolean deleted = false;
      deleted = knativeClient
        .triggers()
        .withLabel(labelKey, key)
        .delete();
      if (deleted) LOGGER.info("Deleted trigger: {}", key);
      deleted =knativeClient
        .sequences()
        .withLabel(labelKey, key)
        .delete();
      if (deleted) LOGGER.info("Deleted sequence: {}", key);
      deleted =knativeClient
        .services()
        .withLabel(labelKey, key)
        .delete();
      if (deleted) LOGGER.info("Deleted service: {}", key);
    } else {
      var svcName = "oaas-" + function.getName().replaceAll("[/.]", "-")
        .toLowerCase();
      Service service = createService(function, svcName);
      var oldSvc = knativeClient.services().withName(svcName)
          .get();
      if (oldSvc != null) {
        knativeClient.services()
          .withName(svcName)
          .edit(svc -> {
            svc.setSpec(service.getSpec());
            return svc;
          });
        LOGGER.info("Patched service: {}",service.getMetadata().getName());
      } else {
        if (LOGGER.isDebugEnabled())
          LOGGER.debug("Submitting service: {}", Json.encodePrettily(service));
        knativeClient.services().create(service);
        LOGGER.info("Created service: {}",service.getMetadata().getName());
      }

      Sequence sequence = createSequence(function, svcName);
      var oldSq = knativeClient.sequences().withName(svcName+ "-sequence")
        .get();
      if (oldSq == null) {
        knativeClient.sequences().createOrReplace(sequence);
        LOGGER.info("Created sequence: {}",sequence.getMetadata().getName());
      }

      Trigger trigger = createTrigger(function, svcName);
      var oldTg = knativeClient.triggers().withName(svcName+ "-trigger")
        .get();
      if (oldTg == null) {
        knativeClient.triggers().create(trigger);
        LOGGER.info("Created trigger: {}",trigger.getMetadata().getName());
      }

    }
  }

  private Trigger createTrigger(OaasFunctionDto function,
                                String svcName) {
    return new TriggerBuilder()
      .withNewMetadata()
      .withName(svcName + "-trigger")
      .addToLabels(labelKey, function.getName())
      .addToAnnotations("knative-eventing-injection", "enabled")
      .endMetadata()
      .withNewSpec()
      .withBroker("default")
      .withNewFilter()
      .addToAttributes("kafkaheadercetype", "oaas.task")
      .addToAttributes("kafkaheadercefunction", function.getName())
      .addToAttributes("kafkaheadercetasktype", "DURABLE")
      .endFilter()
      .withNewSubscriber()
      .withNewRef(
        "flows.knative.dev/v1",
        "Sequence",
        svcName + "-sequence",
        knativeClient.getNamespace()
      )
      .endSubscriber()
      .endSpec()
      .build();
  }

  private Sequence createSequence(OaasFunctionDto function,
                                  String svcName) {
    var step = new SequenceStepBuilder()
      .withNewRef(
        "serving.knative.dev/v1",
        "Service",
        svcName,
        knativeClient.getNamespace()
      )
//      .withNewDelivery()
//      .withNewDeadLetterSink()
//      .withNewRef(
//        "v1",
//        "Service",
//        taskHandler,
//        knativeClient.getNamespace()
//      )
//      .endDeadLetterSink()
//      .endDelivery()
      .build();
    return new SequenceBuilder()
      .withNewMetadata()
      .withName(svcName + "-sequence")
      .addToLabels(labelKey, function.getName())
      .endMetadata()
      .withNewSpec()
      .withNewChannelTemplate()
      .withApiVersion("messaging.knative.dev/v1")
      .withKind("InMemoryChannel")
      .endChannelTemplate()
      .addToSteps(step)
      .withNewReply()
      .withNewRef(
        "v1",
        "Service",
        taskHandler,
        knativeClient.getNamespace()
      )
      .endReply()
      .endSpec()
      .build();
  }

  private Service createService(OaasFunctionDto function,
                                String svcName) {
    var provision = function.getProvision().getKnative();
    var annotation = new HashMap<String, String>();
    if (provision.getMinScale() >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(provision.getMinScale()));
    if (provision.getMaxScale() >= 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(provision.getMaxScale()));
    Map<String, Quantity> requests = new HashMap<>();
    if (provision.getRequestsCpu()!=null) {
      requests.put("cpu", Quantity.parse(provision.getRequestsCpu()));
    }
    if (provision.getRequestsMemory()!=null) {
      requests.put("memory", Quantity.parse(provision.getRequestsMemory()));
    }
    Map<String, Quantity> limits = new HashMap<>();
    if (provision.getLimitsCpu()!=null) {
      limits.put("cpu", Quantity.parse(provision.getLimitsCpu()));
    }
    if (provision.getLimitsMemory()!=null) {
      limits.put("memory", Quantity.parse(provision.getLimitsMemory()));
    }
    Container container = new ContainerBuilder()
      .withImage(provision.getImage())
      .withNewResources()
      .withLimits(limits)
      .withRequests(requests)
      .endResources()
      .build();

    var labels = new HashMap<String,String>();
    labels.put(labelKey, function.getName());
    if (!exposeKnative) {
      labels.put("networking.knative.dev/visibility","cluster-local");
    }

    return new ServiceBuilder()
      .withNewMetadata()
      .withName(svcName)
      .addToLabels(labels)
      .endMetadata()
      .withNewSpec()
      .withNewTemplate()
      .withNewMetadata()
      .withAnnotations(annotation)
      .endMetadata()
      .withNewSpec()
      .withContainerConcurrency(provision.getConcurrency() > 0 ?
        (long) provision.getConcurrency():null)
      .withContainers(container)
      .endSpec()
      .endTemplate()
      .endSpec()
      .build();
  }
}
