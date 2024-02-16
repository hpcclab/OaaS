package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.Set;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class InvokerManager {
  private final ClassControllerRegistry registry;
  private final VerticleDeployer verticleDeployer;
  private final Set<String> managedCls = Sets.mutable.empty();

  @Inject
  public InvokerManager(ClassControllerRegistry registry,
                        VerticleDeployer verticleDeployer) {
    this.registry = registry;
    this.verticleDeployer = verticleDeployer;
  }

  Uni<ClassController> registerManaged(ProtoOClass cls) {
    managedCls.add(cls.getKey());
    return registry.registerOrUpdate(cls)
      .call(() -> verticleDeployer.handleCls(cls));
  }

  Uni<Void> update(OClass cls) {
    return registry.registerOrUpdate(cls)
      .replaceWithVoid();
  }

  Uni<Void> update(OFunction fn) {
    registry.updateFunction(fn);
    return Uni.createFrom().nullItem();
  }

  public ClassControllerRegistry getRegistry() {
    return registry;
  }

  public Set<String> getManagedCls() {
    return managedCls;
  }
}
