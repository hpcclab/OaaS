package org.hpcclab.oaas.model.state;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoEnumValue;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasObjectState implements Serializable {
  @ProtoField(1)
  StateType type;
  @ProtoField(2)
  String baseUrl;
  @ProtoField(3)
  List<String> keys;
  @JsonRawValue
  @ProtoField(4)
  String records;

  public enum StateType {
    @ProtoEnumValue(0)
    FILE,
    @ProtoEnumValue(1)
    FILES,
    @ProtoEnumValue(2)
    SEGMENTABLE,
    @ProtoEnumValue(3)
    RECORD
  }
}
