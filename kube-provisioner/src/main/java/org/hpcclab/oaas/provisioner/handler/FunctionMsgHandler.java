package org.hpcclab.oaas.provisioner.handler;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1.Trigger;
import io.fabric8.knative.eventing.v1.TriggerFilter;
import io.fabric8.knative.eventing.v1.TriggerSpec;
import io.fabric8.knative.flows.v1.*;
import io.fabric8.knative.internal.pkg.apis.duck.v1.Destination;
import io.fabric8.knative.internal.pkg.apis.duck.v1.KReference;
import io.fabric8.knative.messaging.v1.ChannelTemplateSpec;
import io.fabric8.knative.serving.v1.*;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.kafka.Record;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionDeploymentStatus;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.provisioner.KpConfig;
import org.hpcclab.oaas.provisioner.provisioner.KnativeProvisioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.hpcclab.oaas.provisioner.FunctionWatcher.extractReadyCondition;
import static org.hpcclab.oaas.provisioner.KpConfig.LABEL_KEY;

@ApplicationScoped
public class FunctionMsgHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(FunctionMsgHandler.class);
  @Inject
  ArgFunctionRepository functionRepo;
  @Inject
  KnativeProvisioner knativeProvisioner;

  @Incoming("fnProvisions")
  @RunOnVirtualThread
  public void handle(Record<String, OaasFunction> functionRecord) {
    var key = functionRecord.key();
    LOGGER.debug("Received Knative provision: {}", key);
    var function = functionRecord.value();
    if (function==null) {
      return;
    }

    if (function.getProvision()!=null && function.getProvision().getKnative()!=null) {
      var functionUpdater = knativeProvisioner.provision(functionRecord);
      functionRepo.compute(function.getKey(), (k, f) -> {
        functionUpdater.accept(f);
        return f;
      });
    }
  }
}
