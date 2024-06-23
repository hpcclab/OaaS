package org.hpcclab.oaas.crm.env;

import io.fabric8.kubernetes.api.model.Quantity;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;

@Builder(toBuilder = true)
public record OprcEnvironment(
  Config config,
  EnvResource total,
  EnvResource usable,
  EnvResource request,
  AvailabilityInfo availability
) {
  @Builder(toBuilder = true)
  public record Config(String namespace,
                       String kafkaBootstrap,
                       String classManagerHost,
                       String classManagerPort,
                       String fnTopic,
                       String clsTopic,
                       String crHashTopic,
                       boolean exposeKnative,
                       boolean useKnativeLb,
                       boolean feasibleCheckDisable,
                       String logLevel) {
  }

  public record EnvResource(double cpu,
                            long mem) {
    public static final EnvResource ZERO = new EnvResource(0d, 0L);
    static final Quantity QUANTITY_ZERO = Quantity.parse("0");

    public EnvResource(BigDecimal cpu, BigDecimal mem) {
      this(cpu.doubleValue(), mem.longValue());
    }


    public EnvResource(Map<String, Quantity> m) {
      this(m.getOrDefault("cpu", QUANTITY_ZERO).getNumericalAmount(),
        m.getOrDefault("memory", QUANTITY_ZERO).getNumericalAmount());
    }

    public static EnvResource sum(EnvResource a, EnvResource b) {
      return new EnvResource(a.cpu + b.cpu, a.mem + b.mem);
    }

    public EnvResource subtract(EnvResource resource) {
      return new EnvResource(cpu - resource.cpu, mem - resource.mem);
    }

    public EnvResource mul(int num) {
      return new EnvResource(cpu * num, mem * num);
    }

    public boolean hasMore(EnvResource estimate) {
      return cpu > estimate.cpu && mem > estimate.mem;
    }
  }

  @Builder(toBuilder = true)
  public record AvailabilityInfo(double uptimePercentage) {
  }
}
