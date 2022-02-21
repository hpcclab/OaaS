package org.hpcclab.oaas.model.proto;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
  schemaPackageName = "org.hpcclab.oaas.model.proto",
  basePackages = {
    "org.hpcclab.oaas.model.function",
    "org.hpcclab.oaas.model.object",
    "org.hpcclab.oaas.model.provision",
    "org.hpcclab.oaas.model.state",
    "org.hpcclab.oaas.model.task",
    "org.hpcclab.oaas.model.proto",
  }
)
public interface OaasSchema extends GeneratedSchema {
}
