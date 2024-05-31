package org.hpcclab.oaas.invoker;

import io.smallrye.mutiny.Uni;
import io.vertx.kafka.admin.NewTopic;
import io.vertx.mutiny.kafka.admin.KafkaAdminClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.invocation.controller.ClassController;
import org.hpcclab.oaas.invocation.controller.ClassControllerBuilder;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassConfig;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class InvokerManager {
  final InvokerConfig config;
  final KafkaAdminClient adminClient;
  final ClassControllerBuilder classControllerBuilder;
  private final ClassControllerRegistry registry;
  private final VerticleDeployer verticleDeployer;
  private final Set<String> managedCls = Sets.mutable.empty();

  @Inject
  public InvokerManager(ClassControllerRegistry registry,
                        VerticleDeployer verticleDeployer,
                        InvokerConfig config,
                        KafkaAdminClient adminClient,
                        ClassControllerBuilder classControllerBuilder) {
    this.registry = registry;
    this.verticleDeployer = verticleDeployer;
    this.config = config;
    this.adminClient = adminClient;
    this.classControllerBuilder = classControllerBuilder;
  }

  Uni<ClassController> registerManaged(ProtoOClass cls) {
    managedCls.add(cls.getKey());
    return
      classControllerBuilder.build(cls)
        .invoke(registry::register)
        .call(() -> createTopic(cls))
        .call(() -> verticleDeployer.deployVerticleIfNew(cls));
  }

  Uni<Void> update(OClass cls) {
    return
      classControllerBuilder.build(cls)
        .invoke(registry::register)
        .replaceWithVoid();
  }

  Uni<Void> update(ProtoOClass cls) {
    return
      classControllerBuilder.build(cls)
        .invoke(registry::register)
      .replaceWithVoid();
  }

  Uni<Void> update(OFunction fn) {
    UnaryOperator<FunctionController> updator = classControllerBuilder.createUpdator(fn);
    registry.updateFunction(fn, updator);
    return Uni.createFrom().nullItem();
  }

  Uni<Void> createTopic(ProtoOClass cls) {
    var topicName = config.invokeTopicPrefix() + cls.getKey();
    return adminClient.listTopics()
      .flatMap(topics -> {
        var topicExist = topics.contains(topicName);
        if (!topicExist) {
          var conf = cls.getConfig();
          return adminClient.createTopics(List.of(
            new NewTopic(topicName,
              conf.getPartitions() <= 0 ? OClassConfig.DEFAULT_PARTITIONS:conf.getPartitions(),
              (short) 1)
          ));
        }
        return Uni.createFrom().nullItem();
      });
  }

  public ClassControllerRegistry getRegistry() {
    return registry;
  }

  public Set<String> getManagedCls() {
    return managedCls;
  }
}
