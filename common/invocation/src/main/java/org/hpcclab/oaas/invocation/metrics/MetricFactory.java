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


  interface MetricCounter {
    void increase();
  }

  interface MetricTimer{
    void recordTime(Duration duration);
  }
}
