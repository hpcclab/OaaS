package org.hpcclab.oaas.invocation.applier;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LogicalFunctionApplier implements FunctionApplier {

  private static final Logger LOGGER = LoggerFactory.getLogger( LogicalFunctionApplier.class );
  IdGenerator idGenerator;

  Instance<LogicalSubApplier> applierInstance;
  Map<String, LogicalSubApplier> appliers = Map.of();


  public LogicalFunctionApplier(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  @PostConstruct
  public void setup() {
    appliers = applierInstance.stream()
      .collect(Collectors.toUnmodifiableMap(LogicalSubApplier::functionKey, Function.identity()));
    LOGGER.info("initialize logical appliers {}", appliers.keySet());
  }

  public Uni<InvocationContext> apply(InvocationContext context) {
//    if (context.getFunction().getKey().equals("builtin.logical.copy")) {
//      var o = context.getMain().copy();
//      o.setId(idGenerator.generate(context));
//      context.setOutput(o);
//      return Uni.createFrom().item(context);
//    } else {
//      return null;
//    }

    var applier = appliers.get(context.getFunction().getKey());
    return applier.apply(context);
  }

  public void validate(InvocationContext context) {
    var applier = appliers.get(context.getFunction().getKey());
    if (applier == null)
      throw new FunctionValidationException("No logical function implemented");
    applier.validate(context);
  }
}
