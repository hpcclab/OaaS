package org.hpcclab.oaas.crm.controller;


import com.google.protobuf.ByteString;
import io.fabric8.kubernetes.api.model.*;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.proto.KnativeProvisionOrBuilder;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class K8sResourceUtil {
  public static ResourceRequirements makeResourceRequirements(CrInstanceSpec spec) {
    Map<String, Quantity> requests = new HashMap<>();
    if (spec.requestsCpu() > 0) {
      requests.put("cpu",
        Quantity.fromNumericalAmount(BigDecimal.valueOf(spec.requestsCpu()), null)
      );
    }
    if (spec.requestsMemory() > 0) {
      requests.put("memory",
        Quantity.fromNumericalAmount(BigDecimal.valueOf(spec.requestsMemory()), null)
      );
    }
    Map<String, Quantity> limits = new HashMap<>();
    if (spec.limitsCpu() > 0) {
      limits.put("cpu",
        Quantity.fromNumericalAmount(BigDecimal.valueOf(spec.limitsCpu()), null)
      );
    }
    if (spec.limitsMemory() > 0) {
      limits.put("memory",
        Quantity.fromNumericalAmount(BigDecimal.valueOf(spec.limitsMemory()), null)
      );
    }
    return new ResourceRequirementsBuilder()
      .withRequests(requests)
      .withLimits(limits)
      .build();
  }


  public static Map<String, String> makeAnnotation(Map<String, String> annotation,
                                                   KnativeProvisionOrBuilder knConf,
                                                   CrtMappingConfig.FnConfig fnConfig) {
    if (knConf.getMinScale() >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(knConf.getMinScale()));
    if (knConf.getMaxScale() > 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(knConf.getMaxScale()));
    if (!knConf.getScaleDownDelay().isEmpty())
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        knConf.getScaleDownDelay());
    else if (fnConfig.defaultScaleDawnDelay()!=null && !fnConfig.defaultScaleDawnDelay().isEmpty()) {
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        fnConfig.defaultScaleDawnDelay());
    }
    if (knConf.getTargetConcurrency() > 0)
      annotation.put("autoscaling.knative.dev/target",
        String.valueOf(knConf.getTargetConcurrency()));
    return annotation;
  }

  public static Map<String, String> makeAnnotation(Map<String, String> annotation,
                                                   CrInstanceSpec instance) {

    if (instance.minInstance() >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(instance.minInstance()));
    if (instance.maxInstance() >= 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(instance.maxInstance()));
    if (instance.scaleDownDelay()!=null && !instance.scaleDownDelay().isEmpty())
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        instance.scaleDownDelay());
    if (instance.targetConcurrency() > 0)
      annotation.put("autoscaling.knative.dev/target",
        String.valueOf(instance.targetConcurrency()));
    return annotation;
  }

  public static GenericKubernetesResource createPodMonitor(String name,
                                                           String namespace,
                                                           Map<String, String> labels) {

    var spec = new HashMap<>();
    spec.put("selector", Map.of("matchLabels", labels)
    );
    spec.put("podMetricsEndpoints", List.of(
        Map.of(
          "port", "http",
          "path", "/q/metrics",
          "interval", "20s"
        )
      )
    );

    return new GenericKubernetesResourceBuilder()
      .withKind("PodMonitor")
      .withApiVersion("monitoring.coreos.com/v1")
      .withNewMetadata()
      .withName(name)
      .withNamespace(namespace)
      .withLabels(labels)
      .endMetadata()
      .withAdditionalProperties(Map.of("spec", spec))
      .build();
  }


  public static List<EnvVar> extractEnv(ProtoOFunction function) {
    ByteString custom = function.getConfig().getCustom();
    if (custom.isEmpty())
      return List.of();
    JsonObject entries = new JsonObject(custom.toStringUtf8());
    return entries.stream()
      .map(e -> new EnvVar(e.getKey().toUpperCase(), e.getValue().toString(), null))
      .toList();
  }

}
