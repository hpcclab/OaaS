package org.hpcclab.oaas.provisioner.provisioner;

import io.quarkus.kafka.client.runtime.KafkaAdminClient;
import io.quarkus.kafka.client.runtime.devui.model.request.KafkaCreateTopicRequest;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.provisioner.KpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@ApplicationScoped
public class KafkaProvisioner implements Provisioner<OaasClass>{
private static final Logger logger = LoggerFactory.getLogger( KafkaProvisioner.class );
  @Inject
  KafkaAdminClient adminClient;
  @Inject
  KpConfig config;

  @Override
  public Consumer<OaasClass> provision(Record<String, OaasClass> rec) {
    var cls = rec.value();
    var topicName = config.invokeTopicPrefix() + cls.getKey();
    var req = new KafkaCreateTopicRequest(topicName, cls.getConfig().getPartitions(), (short) cls.getConfig().getReplicas());
    var success = adminClient.createTopic(req);
    if (!success)
      logger.error("can not create topic for cls {}", cls.getKey());
    return null;
  }
}
