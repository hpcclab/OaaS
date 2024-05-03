package org.hpcclab.oaas.crm.optimize;

import org.hpcclab.oaas.crm.CrtMappingConfig;
import org.hpcclab.oaas.crm.controller.CrController;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.SvcPerformanceMetrics;
import org.hpcclab.oaas.proto.ProtoQosRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.mean;

public class CpuBasedQoSOptimizer extends AbstractQoSOptimizer {
  private static final Logger logger = LoggerFactory.getLogger(CpuBasedQoSOptimizer.class);

  public CpuBasedQoSOptimizer(CrtMappingConfig.CrtConfig crtConfig) {
    super(crtConfig);
  }

  protected AdjustComponent adjustComponent(CrController controller,
                                          CrInstanceSpec instanceSpec,
                                          CrtMappingConfig.ScalingConfig svcConfig,
                                          ProtoQosRequirement qos,
                                          SvcPerformanceMetrics metrics,
                                          String name,
                                          boolean isFunc) {
    if (metrics==null)
      return AdjustComponent.NONE;
    if (svcConfig.disableDynamicAdjustment())
      return AdjustComponent.NONE;
    int targetRps = qos.getThroughput();
    if (targetRps <= 0)
      return AdjustComponent.NONE;
    metrics = metrics.filterByStableTime(controller.getStableTime(name) + 60000);
    metrics = metrics.syncTimeOnCpuMemRps();
    if (metrics.rps().size() < minDatapoints)
      return AdjustComponent.NONE;

    double meanRps = mean(metrics.rps());
    double meanCpu = mean(metrics.cpu());
    var cpuPerRps = meanCpu / meanRps;
    if (meanRps < 1 || cpuPerRps == 0) {// < 1 is too little. Preventing result explode
      return AdjustComponent.NONE; // prevent overriding throughput guarantee
    }

    var objectiveAmplifier = svcConfig.objectiveAmplifier() <= 0 ? 1:svcConfig.objectiveAmplifier();
    var objectiveMissThreshold = svcConfig.objectiveMissThreshold() <= 0 ? 1:svcConfig.objectiveMissThreshold();
    var idleFilterThreshold = svcConfig.idleFilterThreshold() <= 0 ? 0.4:svcConfig.idleFilterThreshold();

    var totalRequestCpu = Math.max(1, instanceSpec.minInstance()) * instanceSpec.requestsCpu();
    double expectedCpu = Math.max(0, cpuPerRps * targetRps * objectiveAmplifier);
    int expectedInstance = (int) Math.ceil(expectedCpu / instanceSpec.requestsCpu()); // or limit?
    var cpuPercentage = meanCpu / totalRequestCpu;
    var lower = isFunc ? fnThresholdLower:thresholdLower;
    var upper = isFunc ? fnThresholdUpper:thresholdUpper;
    var expectedRps = cpuPercentage < 1 ? meanRps / cpuPercentage : meanRps;
    var prevInstance = instanceSpec.minInstance();
    var nextInstance = prevInstance;
    var missRpsThreshold = targetRps * objectiveMissThreshold;
    logger.debug("compute adjust[1] on ({} : {})[{}], meanRps {}, expectedRps {} (>{}), "
        + "meanCpu {}, cpuPerRps {}, targetRps {}, cpuPercentage {} ({}|{}|{}), expectedInstance {}",
      controller.getTsidString(), name, metrics.rps().size(), meanRps, expectedRps,
      missRpsThreshold, meanCpu, cpuPerRps,
      targetRps, cpuPercentage,  lower, idleFilterThreshold, upper, expectedInstance);

    /*
    * over provisioning
    ++ low utilization (cpu-util < thresh_low_util)  while meet the objective (real rps > objective)
    * under provisioning
    ++ high utilization (cpu-util > thresh_high_util) while not meet the objective (real rps < objective)
    ++ expect that objective is not fulfilment by current resource ( expect_rps / objective < thresh_objective_fulfilment) with the current utilization higher than threshold
     */
    if (cpuPercentage < lower && meanRps > targetRps) {
      nextInstance = Math.min(expectedInstance, nextInstance);
    } else if (cpuPercentage > upper && meanRps < targetRps) {
      nextInstance = Math.max(expectedInstance, nextInstance);
    } else if (expectedRps < missRpsThreshold && cpuPercentage > idleFilterThreshold) {
      nextInstance = Math.max(expectedInstance, nextInstance);
    } else {
      return AdjustComponent.NONE;
    }

    int capChanged = limitChange(instanceSpec.minInstance(), nextInstance, svcConfig.maxScaleStep());
    capChanged = Math.max(capChanged, instanceSpec.minAvail());
    if (instanceSpec.maxInstance() > 0) {
      capChanged = Math.min(capChanged, instanceSpec.maxInstance());
    }
    var adjust = instanceSpec.toBuilder().minInstance(capChanged).build();
    var needChange = !instanceSpec.equals(adjust);
    logger.debug("compute adjust[2] on ({} : {}), nextInstance {}, prevInstance {}, maxInstance {}, capChanged {}, needChange {}",
      controller.getTsidString(), name, nextInstance, prevInstance, instanceSpec.maxInstance(), capChanged, needChange);

    if (needChange) {
      logger.debug("compute adjust[3] on ({} : {}), cpu {}, rps {}",
        controller.getTsidString(), name, metrics.cpu(), metrics.rps()
      );
      logger.debug("next adjustment ({} : {}) : {}", controller.getTsidString(), name, adjust);
    }
    return new AdjustComponent(
      needChange,
      adjust
    );
  }

}
