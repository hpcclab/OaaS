package org.hpcclab.oaas.invoker.metrics;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.exception.StdOaasException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class RequestCounterMap {
  final MicrometerMetricFactory factory;
  final ClassControllerRegistry registry;
  final Map<ClsFbPair, MetricFactory.MetricCounter> timerMap = new ConcurrentHashMap<>();

  public RequestCounterMap(MicrometerMetricFactory factory,
                           ClassControllerRegistry registry) {
    this.factory = factory;
    this.registry = registry;
  }

  public void increase(String cls, String fb) {
    ClassController clsController = registry.getClassController(cls);
    if (clsController==null) return;
    ClsFbPair clsFnPair = new ClsFbPair(cls, fb);
    MetricFactory.MetricCounter counter = timerMap.computeIfAbsent(clsFnPair,
      k -> {
        FunctionController fnController = clsController.getFunctionController(fb);
        if (fnController == null) throw InvocationException.notFoundFnInCls(fb, cls);
        String fnKey = fnController.getFunction().getKey();
        return factory.createRequestCounter(cls, fb, fnKey);
      });
    counter.increase();
  }

  public record ClsFbPair(String cls, String fb) {
  }
}
