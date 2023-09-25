package org.hpcclab.oaas.model.proto;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

import static org.hpcclab.oaas.model.proto.OaasSchema.PACKAGE_NAME;

@AutoProtoSchemaBuilder(
  schemaPackageName = PACKAGE_NAME,
  schemaFileName = "oaas.proto",
  schemaFilePath = "/protostream",
  basePackages = {
    "org.hpcclab.oaas.model.function",
    "org.hpcclab.oaas.model.object",
    "org.hpcclab.oaas.model.cls",
    "org.hpcclab.oaas.model.provision",
    "org.hpcclab.oaas.model.state",
    "org.hpcclab.oaas.model.task",
    "org.hpcclab.oaas.model.proto",
    "org.hpcclab.oaas.model.invocation",
  }
)
public interface OaasSchema extends GeneratedSchema {
  String PACKAGE_NAME = "oaas";
  static String makeFullName(Class<?> cls) {
    return PACKAGE_NAME + '.' + cls.getSimpleName();
  }
}
