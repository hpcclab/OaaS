package org.hpcclab.oaas.crm.observe;

import com.github.f4b6a3.tsid.Tsid;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.crm.CrControllerManager;
import org.hpcclab.oaas.crm.env.EnvironmentManager;
import org.hpcclab.oaas.crm.optimize.OperationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@ApplicationScoped
public class ObserverLoopRunner {
  private static final Logger logger = LoggerFactory.getLogger(ObserverLoopRunner.class);
  final Vertx vertx;
  final CrMetricObserver metricObserver;
  final CrControllerManager controllerManager;
  final long interval;
  final Executor executor;
  final OperationExecutor operationExecutor;
  final EnvironmentManager environmentManager;

  @Inject
  public ObserverLoopRunner(Vertx vertx,
                            CrMetricObserver metricObserver,
                            CrControllerManager controllerManager,
                            OperationExecutor operationExecutor,
                            EnvironmentManager environmentManager) {
    this.vertx = vertx;
    this.metricObserver = metricObserver;
    this.controllerManager = controllerManager;
    this.operationExecutor = operationExecutor;
    this.environmentManager = environmentManager;
    this.interval = 10000;
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public void setup() {
    vertx.setPeriodic(interval, id -> executor.execute(this::optimizationLoopExecute));
  }

  public void optimizationLoopExecute() {
    try {
      var metricsMap = metricObserver.observe();
//      if (logger.isDebugEnabled() && !metricsMap.isEmpty())
//        logger.debug("metrics {}", Json.encode(metricsMap));
      for (var entry : metricsMap.entrySet()) {
        var id = Tsid.from(entry.getKey());
        var controller = controllerManager.get(id.toLong());
        if (controller==null || controller.isDeleted())
          continue;
        var plan = controller.getOptimizer().adjust(controller, entry.getValue());
        if (plan.needAction()) {
          var operation = controller.createAdjustmentOperation(plan);
          var env = environmentManager.getEnvironment();
          operationExecutor.applyOrThrow(controller, operation, env);
          controllerManager.saveToRemote(controller);
        }
      }
    } catch (Exception e) {
      logger.error("observe metrics error", e);
    }
  }
}
