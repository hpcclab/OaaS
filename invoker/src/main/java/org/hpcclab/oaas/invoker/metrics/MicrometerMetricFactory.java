package org.hpcclab.oaas.invoker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;

import java.time.Duration;
import java.util.List;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class MicrometerMetricFactory implements MetricFactory {
  final MeterRegistry registry;

  @Inject
  public MicrometerMetricFactory(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public MetricCounter createInvocationCounter(String cls,
                                               String fb,
                                               String func) {
    var crId = ConfigProvider.getConfig()
      .getOptionalValue("oprc.crid", String.class);
    List<Tag> tags = Lists.mutable.of(
      Tag.of("cls", cls),
      Tag.of("fb", fb),
      Tag.of("func", func)
    );
    if (crId.isPresent()) tags.add(Tag.of("crId", crId.get()));
    Counter counter = Counter.builder("oprc.invocation")
      .tags(tags)
      .register(registry);
    return new MmCounter(counter);
  }

  @Override
  public MetricTimer createInvocationTimer(String cls,
                                           String fb,
                                           String func) {
    var crId = ConfigProvider.getConfig()
      .getOptionalValue("oprc.crid", String.class);
    var timerBuilder = Timer.builder("oprc.invocation")
      .tag("cls", cls)
      .tag("fb", fb)
      .tag("func", func)
      .publishPercentileHistogram(true)
      .publishPercentiles(0.50, 0.95, 0.99);
    if (crId.isPresent()) timerBuilder.tag("crId", crId.get());
    return new MmTimer(timerBuilder.register(registry));
  }

  record MmCounter(Counter counter) implements MetricCounter {
    @Override
    public void increase() {
      counter.increment();
    }
  }

  record MmTimer(Timer timer) implements MetricTimer {
    @Override
    public void recordTime(Duration duration) {
      timer.record(duration);
    }
  }
}
