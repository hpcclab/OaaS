package org.hpcclab.oaas.model.proto;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;

import static org.hpcclab.oaas.model.proto.OaasSchema.PACKAGE_NAME;

@ProtoSchema(
  syntax = ProtoSyntax.PROTO3,
  schemaPackageName = PACKAGE_NAME,
  schemaFileName = "oaas.proto",
  schemaFilePath = "/protostream",
  basePackages = {
    "org.hpcclab.oaas.model.cr",
    "org.hpcclab.oaas.model.object",
    "org.hpcclab.oaas.model.state",
    "org.hpcclab.oaas.model.proto",
  }
)
public
interface OaasSchema extends GeneratedSchema {
  String PACKAGE_NAME = "oaas";

  static String makeFullName(Class<?> cls) {
    return PACKAGE_NAME + '.' + cls.getSimpleName();
  }
}
