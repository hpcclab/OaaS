package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ClassConfig {
  @ProtoField(1)
  DSMap options;
  @ProtoField(value = 2, defaultValue = "12")
  int partitions = 12;
  @ProtoField(value = 3, defaultValue = "1")
  int replicas = 1;
  @ProtoField(4)
  String structStore;
  @ProtoField(5)
  String unstructStore;

  public ClassConfig() {
  }

  @ProtoFactory
  public ClassConfig(DSMap options, int partitions, int replicas, String structStore, String unstructStore) {
    this.options = options;
    this.partitions = partitions;
    this.replicas = replicas;
    this.structStore = structStore;
    this.unstructStore = unstructStore;
  }
}
