package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StaticUrlProvision {
  @ProtoField(1)
  String url;

  public StaticUrlProvision() {
  }

  @ProtoFactory
  public StaticUrlProvision(String url) {
    this.url = url;
  }
}
