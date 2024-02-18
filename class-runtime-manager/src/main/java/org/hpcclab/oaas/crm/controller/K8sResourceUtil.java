package org.hpcclab.oaas.crm.controller;


import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;
import org.hpcclab.oaas.proto.KnativeProvisionOrBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
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
                                                    KnativeProvisionOrBuilder knConf) {
    if (knConf.getMinScale() >= 0)
      annotation.put("autoscaling.knative.dev/minScale",
        String.valueOf(knConf.getMinScale()));
    if (knConf.getMaxScale() >= 0)
      annotation.put("autoscaling.knative.dev/maxScale",
        String.valueOf(knConf.getMaxScale()));
    if (!knConf.getScaleDownDelay().isEmpty())
      annotation.put("autoscaling.knative.dev/scale-down-delay",
        knConf.getScaleDownDelay());
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
}
