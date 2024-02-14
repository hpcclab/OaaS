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
public class OClassConfig {
  public static final int DEFAULT_PARTITIONS = 12;

  @ProtoField(1)
  DSMap options;
  @ProtoField(value = 2, defaultValue = "12")
  int partitions = DEFAULT_PARTITIONS;
  @ProtoField(value = 3, defaultValue = "1")
  int replicas = 1;
  @ProtoField(4)
  String structStore;
  @ProtoField(5)
  String unstructStore;
  @ProtoField(6)
  String logStore;
  @ProtoField(value = 7, defaultValue = "true")
  boolean hashAwareSync;
  @ProtoField(value = 8)
  String crTemplate;

  public OClassConfig() {
  }

  @ProtoFactory
  public OClassConfig(DSMap options, int partitions, int replicas, String structStore, String unstructStore,
                      String logStore,
                      boolean hashAwareSync,
                      String crTemplate
                      ) {
    this.options = options;
    this.partitions = partitions;
    this.replicas = replicas;
    this.structStore = structStore;
    this.unstructStore = unstructStore;
    this.logStore = logStore;
    this.hashAwareSync = hashAwareSync;
    this.crTemplate = crTemplate;
  }

  void validate() {
    if (partitions < 1) {
      partitions = 1;
    }
    if (replicas < 1) {
      replicas = 1;
    }
    if (crTemplate == null) crTemplate = "default";
  }
}
