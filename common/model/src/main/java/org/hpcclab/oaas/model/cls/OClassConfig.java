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
  DSMap options;
  int partitions = DEFAULT_PARTITIONS;
  String structStore;
  String unstructStore;
  String logStore;
  String crTemplate;
  boolean disableHpa = false;
  boolean disableHashAware = false;

  public OClassConfig() {
  }


  public OClassConfig(DSMap options,
                      int partitions,
                      String structStore,
                      String unstructStore,
                      String logStore,
                      String crTemplate,
                      boolean disableHpa,
                      boolean disableHashAware) {
    this.options = options;
    this.partitions = partitions;
    this.structStore = structStore;
    this.unstructStore = unstructStore;
    this.logStore = logStore;
    this.crTemplate = crTemplate;
    this.disableHpa = disableHpa;
    this.disableHashAware = disableHashAware;
  }

  void validate() {
    if (partitions < 1) {
      partitions = DEFAULT_PARTITIONS;
    }
  }
}
