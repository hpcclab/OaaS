package org.hpcclab.oaas.provisioner.provisioner;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.serving.v1.Service;
import io.fabric8.knative.serving.v1.ServiceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.Json;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.function.DeploymentCondition;
import org.hpcclab.oaas.model.function.FunctionDeploymentStatus;
import org.hpcclab.oaas.model.function.FunctionState;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.provisioner.KpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.hpcclab.oaas.provisioner.FunctionWatcher.extractReadyCondition;
import static org.hpcclab.oaas.provisioner.KpConfig.LABEL_KEY;

@ApplicationScoped
@RegisterForReflection(
  targets = {
    OFunction.class,
    Service.class
  },
  registerFullHierarchy = true
)
public class KnativeProvisioner implements Provisioner<OFunction> {

  private static final Logger logger = LoggerFactory.getLogger(KnativeProvisioner.class);
  @Inject
  KnativeClient knativeClient;
  @Inject
  KpConfig kpConfig;

  @Override
  public Consumer<OFunction> provision(OFunction function) {
    var key = function.getKey();
    if (function.getState()==FunctionState.REMOVING) {
      boolean deleted;
      deleted = !knativeClient
        .services()
        .withLabel(LABEL_KEY, key)
        .delete()
        .isEmpty();
      if (deleted) logger.info("Deleted service: {}", key);
      return f ->
        f.getDeploymentStatus().setCondition(DeploymentCondition.DELETED);
    } else {
      var svcName = "oaas-" + function.getKey().replaceAll("[/._]", "-")
        .toLowerCase();
      Service service = createService(function, svcName);
      var oldSvc = knativeClient.services().withName(svcName)
        .get();

      Service newSvc;
      if (oldSvc!=null) {
        newSvc = knativeClient.services()
          .withName(svcName)
          .edit(svc -> {
            svc.setSpec(service.getSpec());
            return svc;
          });
        logger.info("Patched service: {}", service.getMetadata().getName());
      } else {
        if (logger.isDebugEnabled())
          logger.debug("Submitting service: {}", Json.encodePrettily(service));
        newSvc = knativeClient.services().resource(service).create();
        logger.info("Created service: {}", service.getMetadata().getName());
      }
      return updateFunctionStatus(newSvc);
    }
  }


  private Consumer<OFunction> updateFunctionStatus(Service service) {
    var condition = extractReadyCondition(service);
    var ready = condition.isPresent() && condition.get().getStatus().equals("True");
    if (ready) {
      return f -> {
        var kn = f.getProvision().getKnative();
        var apiPath = kn.getApiPath();
        if (apiPath==null)
          apiPath = "";
        else if (!apiPath.isEmpty() && !apiPath.startsWith("/"))
          apiPath = "/" + apiPath;
        if (f.getDeploymentStatus()==null) {
          f.setDeploymentStatus(new FunctionDeploymentStatus());
        }
        f.getDeploymentStatus()
          .setCondition(DeploymentCondition.RUNNING)
          .setInvocationUrl(service.getStatus().getUrl() + apiPath)
          .setErrorMsg(null);
      };
    } else {
      return f -> f.getDeploymentStatus()
        .setCondition(DeploymentCondition.DEPLOYING);
    }
  }


  private Service createService(OFunction function,
                                String svcName) {
    var provision = function.getProvision().getKnative();
    var annotation = new HashMap<String, String>();
    if (provision.getMinScale() >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(provision.getMinScale()));
    if (provision.getMaxScale() >= 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(provision.getMaxScale()));
    if (provision.getScaleDownDelay()!=null)
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        provision.getScaleDownDelay());
    if (provision.getTargetConcurrency() > 0)
      annotation.put("autoscaling.knative.dev/main",
        String.valueOf(provision.getTargetConcurrency()));
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
    var env = provision.getEnv();
    if (env==null) env = DSMap.of();
    var envList = env.entrySet()
      .stream()
      .map(entry -> new EnvVar(entry.getKey(), entry.getValue(), null))
      .toList();
    Container container = new ContainerBuilder()
      .withImage(provision.getImage())
      .withNewResources()
      .withLimits(limits)
      .withRequests(requests)
      .endResources()
      .withEnv(envList)
      .build();

    var labels = new HashMap<String, String>();
    labels.put(LABEL_KEY, function.getKey());
    if (!kpConfig.exposeKnative()) {
      labels.put("networking.knative.dev/visibility", "cluster-local");
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
      .addToLabels(LABEL_KEY, function.getKey())
      .endMetadata()
      .withNewSpec()
      .withTimeoutSeconds(600L)
      .withContainerConcurrency(provision.getConcurrency() > 0 ?
        (long) provision.getConcurrency():null)
      .withContainers(container)
      .endSpec()
      .endTemplate()
      .endSpec()
      .build();
  }
}
