package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoField;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record InvocationStats(
  long queTs,
  long smtTs,
  long cptTs
) {
}
