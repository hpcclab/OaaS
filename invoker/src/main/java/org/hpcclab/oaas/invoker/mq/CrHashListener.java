package org.hpcclab.oaas.invoker.mq;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.proto.ProtoCrHash;

@ApplicationScoped
public class CrHashListener extends NoGroupKafkaConsumer<ProtoCrHash> {
  @Inject
  public CrHashListener(Vertx vertx,
                        InvokerConfig config) {
    super(vertx, config, config.crHashTopic());
  }

  @Override
  ProtoCrHash deserialize(Buffer buffer) {
    try {
      return ProtoCrHash.parseFrom(buffer.getBytes());
    } catch (InvalidProtocolBufferException e) {
      throw new StdOaasException("ProtoCrHash parsing error", e);
    }
  }
}
