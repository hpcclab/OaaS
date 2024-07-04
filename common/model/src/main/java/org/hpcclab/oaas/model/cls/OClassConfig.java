package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OClassConfig {
  public static final int DEFAULT_PARTITIONS = 12;
  int partitions = DEFAULT_PARTITIONS;
  String structStore;
  String unstructStore;
  String logStore;
  String crTemplate;
  boolean disableHashAware = false;

  public OClassConfig() {
  }


  public OClassConfig(int partitions,
                      String structStore,
                      String unstructStore,
                      String logStore,
                      String crTemplate) {
    this.partitions = partitions;
    this.structStore = structStore;
    this.unstructStore = unstructStore;
    this.logStore = logStore;
    this.crTemplate = crTemplate;
  }

  void validate() {
    if (partitions < 1) {
      partitions = DEFAULT_PARTITIONS;
    }
  }
}
