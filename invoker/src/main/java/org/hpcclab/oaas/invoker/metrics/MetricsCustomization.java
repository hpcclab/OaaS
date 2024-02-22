package org.hpcclab.oaas.invoker.metrics;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hpcclab.oaas.invoker.InvokerConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pawissanutt
 */
@Singleton
public class MetricsCustomization {
//  @Produces
//  @Singleton
  public MeterFilter configureAllRegistries() {
    var crId = ConfigProvider.getConfig()
      .getOptionalValue("oprc.crid", String.class);
    List<Tag> tags = new ArrayList<>();
    if (crId.isPresent()) {
      tags.add(Tag.of("crId", crId.get()));
    }
    return MeterFilter.commonTags(tags);
  }
}
