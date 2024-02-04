package org.hpcclab.oaas.crm.env;

import io.fabric8.kubernetes.api.model.Quantity;

import java.math.BigDecimal;
import java.util.Map;

public record OprcEnvironment (
  Config config,
  EnvResource total,
  EnvResource usable
){
  public record Config (String kafkaBootstrap,
                        String classManagerHost,
                        String classManagerPort,
                        boolean exposeKnative) {}
  public record EnvResource(double cpu,
                            long mem){
    static final Quantity QUANTITY_ZERO = Quantity.parse("0");
    public static final EnvResource ZERO = new EnvResource(0d, 0L);

    public EnvResource(BigDecimal cpu, BigDecimal mem) {
      this(cpu.doubleValue(), mem.longValue());
    }


    public EnvResource(Map<String, Quantity> m) {
      this(m.getOrDefault("cpu", QUANTITY_ZERO).getNumericalAmount(),
        m.getOrDefault("memory",QUANTITY_ZERO).getNumericalAmount());
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
}
