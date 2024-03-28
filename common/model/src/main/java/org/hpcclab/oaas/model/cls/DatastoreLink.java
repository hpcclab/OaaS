package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Getter
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DatastoreLink {
  @ProtoField(1)
  String name;
  @ProtoField(2)
  String colName;

  @ProtoFactory
  public DatastoreLink(String name, String colName) {
    this.name = name;
    this.colName = colName;
  }
}
