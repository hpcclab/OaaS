package org.hpcclab.oaas.invocation.metrics;

import java.time.Duration;

/**
 * @author Pawissanutt
 */
public interface MetricFactory {
  MetricCounter createRequestCounter(String cls,
                                     String fb,
                                     String func);

  MetricTimer createInvocationTimer(String cls,
                                    String fb,
                                    String func);

  class NoOpMetricFactory implements MetricFactory {
    @Override
    public MetricCounter createRequestCounter(String cls, String fb, String func) {
      return () -> {};
    }

    @Override
    public MetricTimer createInvocationTimer(String cls, String fb, String func) {
      return duration -> {};
    }
  }

  interface MetricCounter {
    void increase();
  }

  interface MetricTimer{
    void recordTime(Duration duration);
  }
}
