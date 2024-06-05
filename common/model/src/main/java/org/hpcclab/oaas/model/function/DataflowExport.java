package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataflowExport implements Serializable {
  private String from;
  private String as;

  public DataflowExport() {
  }

  @ProtoFactory
  public DataflowExport(String from, String as) {
    this.from = from;
    this.as = as;
  }

  @ProtoField(1)
  public String getFrom() {
    return from;
  }

  @ProtoField(2)
  public String getAs() {
    return as;
  }
}
