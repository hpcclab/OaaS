package org.hpcclab.oaas.invoker.ispn.lookup;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;


@AutoProtoSchemaBuilder(
        schemaPackageName = "oprc",
        schemaFileName = "data.proto",
        schemaFilePath = "/proto",
        includeClasses = {
                ApiAddress.class,
        }
)
public interface DataSchema extends GeneratedSchema {
}
